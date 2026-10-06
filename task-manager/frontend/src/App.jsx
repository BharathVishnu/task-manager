import React, { useState, useEffect, useCallback } from "react";
import {
  FolderKanban,
  CheckCircle2,
  Clock,
  AlertCircle,
  Plus,
  RefreshCw,
  LogOut,
  Link2,
  GitBranch,
  ShieldCheck,
  UserCheck
} from "lucide-react";

const API_BASE = "http://localhost:8082";

export default function App() {
  // Auth state
  const [token, setToken] = useState(null);
  const [userEmail, setUserEmail] = useState("");
  const [authForm, setAuthForm] = useState({ email: "", password: "", isSignup: false });

  // Domain state
  const [projects, setProjects] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState(null);
  const [tasks, setTasks] = useState([]);
  
  // Form inputs
  const [newProjectName, setNewProjectName] = useState("");
  const [newTaskTitle, setNewTaskTitle] = useState("");
  const [depTargetTaskId, setDepTargetTaskId] = useState("");
  const [dependsOnTaskId, setDependsOnTaskId] = useState("");

  // Feedback notifications
  const [notification, setNotification] = useState(null);
  const [loading, setLoading] = useState(false);

  const notify = (msg, type = "info") => {
    setNotification({ msg, type });
    setTimeout(() => setNotification(null), 4500);
  };

  // Authenticated fetch wrapper handling token injection & cookie passing
  const apiCall = useCallback(
    async (path, method = "GET", body = null, customToken = token) => {
      const headers = { "Content-Type": "application/json" };
      if (customToken) {
        headers["Authorization"] = `Bearer ${customToken}`;
      }

      const res = await fetch(`${API_BASE}${path}`, {
        method,
        headers,
        credentials: "include", // Transmits refresh token cookie
        body: body ? JSON.stringify(body) : null,
      });

      const data = await res.json().catch(() => null);

      if (!res.ok) {
        const errorMsg = data?.message || data?.error || `Request failed with status ${res.status}`;
        throw { status: res.status, message: errorMsg, code: data?.code };
      }
      return data;
    },
    [token]
  );

  // 1. Silent token refresh on initial app load
  useEffect(() => {
    const restoreSession = async () => {
      try {
        const res = await fetch(`${API_BASE}/auth/refresh`, {
          method: "POST",
          credentials: "include",
        });
        if (res.ok) {
          const data = await res.json();
          setToken(data.accessToken);
          notify("Session restored via refresh token rotation", "success");
        }
      } catch {
        // No valid session cookie found; user remains logged out
      }
    };
    restoreSession();
  }, []);

  // 2. Load projects when token changes
  const loadProjects = useCallback(async () => {
    if (!token) return;
    try {
      const res = await apiCall("/projects");
      const list = Array.isArray(res) ? res : res.content || [];
      setProjects(list);
      if (list.length > 0 && !selectedProjectId) {
        setSelectedProjectId(list[0].id);
      }
    } catch {
      // Endpoint may return empty set if none created
    }
  }, [token, apiCall, selectedProjectId]);

  useEffect(() => {
    loadProjects();
  }, [loadProjects]);

  // 3. Load tasks for active project
  const loadTasks = useCallback(async () => {
    if (!token || !selectedProjectId) return;
    try {
      const res = await apiCall(`/projects/${selectedProjectId}/tasks`);
      setTasks(Array.isArray(res) ? res : res.content || []);
    } catch (err) {
      notify(`Failed to fetch tasks: ${err.message}`, "error");
    }
  }, [token, selectedProjectId, apiCall]);

  useEffect(() => {
    loadTasks();
  }, [loadTasks]);

  // Authentication Handlers
  const handleAuth = async (e) => {
    e.preventDefault();
    setLoading(true);
    const endpoint = authForm.isSignup ? "/auth/signup" : "/auth/login";
    try {
      const res = await apiCall(endpoint, "POST", {
        email: authForm.email,
        password: authForm.password,
      });

      if (authForm.isSignup) {
        notify("Account registered! Please log in.", "success");
        setAuthForm((prev) => ({ ...prev, isSignup: false }));
      } else {
        setToken(res.accessToken);
        setUserEmail(authForm.email);
        notify("Authenticated successfully", "success");
      }
    } catch (err) {
      notify(err.message, "error");
    } finally {
      setLoading(false);
    }
  };

  const handleManualRefresh = async () => {
    try {
      const res = await apiCall("/auth/refresh", "POST");
      setToken(res.accessToken);
      notify("Access token refreshed & rotated successfully", "success");
    } catch (err) {
      notify(`Token refresh failed: ${err.message}`, "error");
      setToken(null);
    }
  };

  // Domain Handlers
  const handleCreateProject = async (e) => {
    e.preventDefault();
    if (!newProjectName.trim()) return;
    try {
      const proj = await apiCall("/projects", "POST", { name: newProjectName });
      setProjects((prev) => [...prev, proj]);
      setSelectedProjectId(proj.id);
      setNewProjectName("");
      notify(`Project "${proj.name}" created`, "success");
    } catch (err) {
      notify(err.message, "error");
    }
  };

  const handleCreateTask = async (e) => {
    e.preventDefault();
    if (!newTaskTitle.trim() || !selectedProjectId) return;
    try {
      const task = await apiCall(`/projects/${selectedProjectId}/tasks`, "POST", {
        title: newTaskTitle,
      });
      setTasks((prev) => [...prev, task]);
      setNewTaskTitle("");
      notify(`Task created (Version ${task.version ?? 0})`, "success");
    } catch (err) {
      notify(err.message, "error");
    }
  };

  // Optimistic concurrency update
  const handleUpdateStatus = async (task, nextStatus) => {
    try {
      const updated = await apiCall(`/tasks/${task.id}`, "PUT", {
        title: task.title,
        status: nextStatus,
        version: task.version, // Required for optimistic lock check
      });
      setTasks((prev) => prev.map((t) => (t.id === task.id ? updated : t)));
      notify(`Task updated to ${nextStatus} (New Version: ${updated.version})`, "success");
    } catch (err) {
      if (err.status === 409) {
        notify("Version conflict (409): Record modified by another session. Refreshing...", "error");
        loadTasks();
      } else if (err.status === 422) {
        notify(`Action Blocked (422): ${err.message}`, "error");
      } else {
        notify(err.message, "error");
      }
    }
  };

  // Dependency Management & Cycle Verification
  const handleAddDependency = async (e) => {
    e.preventDefault();
    if (!depTargetTaskId || !dependsOnTaskId) return;
    try {
      await apiCall(`/tasks/${depTargetTaskId}/dependencies`, "POST", {
        dependsOnId: Number(dependsOnTaskId),
      });
      notify(`Dependency created: Task ${depTargetTaskId} waits on Task ${dependsOnTaskId}`, "success");
      setDepTargetTaskId("");
      setDependsOnTaskId("");
      loadTasks();
    } catch (err) {
      if (err.status === 422) {
        notify(`Cycle Detected (422): Circular dependency prevented`, "error");
      } else {
        notify(err.message, "error");
      }
    }
  };

  // Auth Screen
  if (!token) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center p-4">
        <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-xl p-8 shadow-2xl backdrop-blur">
          <div className="flex items-center gap-3 mb-6">
            <div className="p-2 bg-indigo-600/20 text-indigo-400 rounded-lg">
              <FolderKanban className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-tight">Task Platform</h1>
              <p className="text-xs text-slate-400">Spring Boot + Neon PostgreSQL</p>
            </div>
          </div>

          {notification && (
            <div
              className={`mb-4 p-3 rounded-lg text-sm border flex items-center gap-2 ${
                notification.type === "error"
                  ? "bg-rose-950/60 border-rose-800 text-rose-300"
                  : "bg-emerald-950/60 border-emerald-800 text-emerald-300"
              }`}
            >
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{notification.msg}</span>
            </div>
          )}

          <form onSubmit={handleAuth} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1">
                Email Address
              </label>
              <input
                type="email"
                required
                value={authForm.email}
                onChange={(e) => setAuthForm({ ...authForm, email: e.target.value })}
                placeholder="developer@domain.com"
                className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3.5 py-2 text-sm text-slate-100 focus:outline-none focus:border-indigo-500"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1">
                Password
              </label>
              <input
                type="password"
                required
                value={authForm.password}
                onChange={(e) => setAuthForm({ ...authForm, password: e.target.value })}
                placeholder="••••••••••••"
                className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3.5 py-2 text-sm text-slate-100 focus:outline-none focus:border-indigo-500"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white rounded-lg font-medium text-sm transition"
            >
              {loading ? "Processing..." : authForm.isSignup ? "Sign Up" : "Log In"}
            </button>

            <button
              type="button"
              onClick={() => setAuthForm((prev) => ({ ...prev, isSignup: !prev.isSignup }))}
              className="w-full text-center text-xs text-slate-400 hover:text-indigo-400 transition"
            >
              {authForm.isSignup
                ? "Already have an account? Sign in"
                : "Need an account? Sign up"}
            </button>
          </form>
        </div>
      </div>
    );
  }

  // Dashboard Interface
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      {/* Top Navbar */}
      <header className="h-16 border-b border-slate-800 bg-slate-900/50 backdrop-blur px-6 flex items-center justify-between sticky top-0 z-20">
        <div className="flex items-center gap-3">
          <FolderKanban className="w-5 h-5 text-indigo-400" />
          <span className="font-bold tracking-tight text-sm">Task Manager Engine</span>
          <span className="text-xs bg-slate-800 px-2 py-0.5 rounded text-slate-400 border border-slate-700">
            Port 8082
          </span>
        </div>

        <div className="flex items-center gap-3">
          {userEmail && (
            <div className="flex items-center gap-2 text-xs text-slate-400 bg-slate-800/60 px-3 py-1.5 rounded-md border border-slate-700">
              <UserCheck className="w-3.5 h-3.5 text-emerald-400" />
              <span>{userEmail}</span>
            </div>
          )}

          <button
            onClick={handleManualRefresh}
            title="Rotate Refresh Cookie"
            className="flex items-center gap-1.5 px-3 py-1.5 text-xs bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-md transition"
          >
            <RefreshCw className="w-3.5 h-3.5 text-indigo-400" />
            <span>Rotate Token</span>
          </button>

          <button
            onClick={() => {
              setToken(null);
              setUserEmail("");
            }}
            className="flex items-center gap-1.5 px-3 py-1.5 text-xs bg-rose-950/40 hover:bg-rose-900/50 text-rose-300 border border-rose-800/80 rounded-md transition"
          >
            <LogOut className="w-3.5 h-3.5" />
            <span>Sign Out</span>
          </button>
        </div>
      </header>

      {/* Floating Status Notification */}
      {notification && (
        <div className="fixed bottom-6 right-6 z-50 animate-in fade-in slide-in-from-bottom-2">
          <div
            className={`p-3.5 rounded-lg text-xs font-medium border shadow-xl flex items-center gap-2.5 ${
              notification.type === "error"
                ? "bg-rose-950 border-rose-700 text-rose-200"
                : "bg-emerald-950 border-emerald-700 text-emerald-200"
            }`}
          >
            <ShieldCheck className="w-4 h-4 flex-shrink-0" />
            <span>{notification.msg}</span>
          </div>
        </div>
      )}

      {/* Main App Grid */}
      <div className="flex-1 grid grid-cols-12 overflow-hidden">
        {/* Left Sidebar: Projects */}
        <aside className="col-span-3 border-r border-slate-800 bg-slate-900/30 p-5 flex flex-col gap-6">
          <div>
            <h2 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-3">
              Projects
            </h2>
            <form onSubmit={handleCreateProject} className="flex gap-2 mb-4">
              <input
                type="text"
                value={newProjectName}
                onChange={(e) => setNewProjectName(e.target.value)}
                placeholder="New project name..."
                className="flex-1 bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-indigo-500"
              />
              <button
                type="submit"
                className="p-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg transition"
              >
                <Plus className="w-3.5 h-3.5" />
              </button>
            </form>

            <div className="space-y-1 overflow-y-auto max-h-[calc(100vh-280px)]">
              {projects.map((p) => (
                <button
                  key={p.id}
                  onClick={() => setSelectedProjectId(p.id)}
                  className={`w-full text-left px-3.5 py-2.5 rounded-lg text-xs font-medium flex items-center justify-between border transition ${
                    selectedProjectId === p.id
                      ? "bg-indigo-600/10 border-indigo-500/50 text-indigo-300"
                      : "border-transparent text-slate-400 hover:bg-slate-800/50 hover:text-slate-200"
                  }`}
                >
                  <span className="truncate">{p.name}</span>
                  <span className="text-[10px] text-slate-500">ID: {p.id}</span>
                </button>
              ))}
              {projects.length === 0 && (
                <p className="text-xs text-slate-500 italic p-2">No projects created yet.</p>
              )}
            </div>
          </div>

          {/* Dependency Injection Control */}
          <div className="mt-auto border-t border-slate-800 pt-4">
            <h2 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2 flex items-center gap-1.5">
              <GitBranch className="w-3.5 h-3.5 text-indigo-400" />
              <span>Link Dependency</span>
            </h2>
            <form onSubmit={handleAddDependency} className="space-y-2">
              <div className="grid grid-cols-2 gap-2">
                <input
                  type="number"
                  placeholder="Task ID"
                  value={depTargetTaskId}
                  onChange={(e) => setDepTargetTaskId(e.target.value)}
                  className="bg-slate-950 border border-slate-800 rounded px-2.5 py-1.5 text-xs text-slate-200 focus:border-indigo-500"
                />
                <input
                  type="number"
                  placeholder="Waits On ID"
                  value={dependsOnTaskId}
                  onChange={(e) => setDependsOnTaskId(e.target.value)}
                  className="bg-slate-950 border border-slate-800 rounded px-2.5 py-1.5 text-xs text-slate-200 focus:border-indigo-500"
                />
              </div>
              <button
                type="submit"
                className="w-full py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded text-xs transition"
              >
                Assert Dependency
              </button>
            </form>
          </div>
        </aside>

        {/* Right Main Panel: Tasks & Concurrency Control */}
        <main className="col-span-9 p-8 overflow-y-auto">
          {selectedProjectId ? (
            <div className="space-y-6 max-w-4xl mx-auto">
              <div className="flex items-center justify-between border-b border-slate-800 pb-4">
                <div>
                  <h2 className="text-lg font-bold text-slate-100">Task Catalog</h2>
                  <p className="text-xs text-slate-400">
                    Project ID #{selectedProjectId} &bull; Validates Optimistic Version Locking & DAG Cycles
                  </p>
                </div>

                <form onSubmit={handleCreateTask} className="flex gap-2">
                  <input
                    type="text"
                    placeholder="New task title..."
                    value={newTaskTitle}
                    onChange={(e) => setNewTaskTitle(e.target.value)}
                    className="w-64 bg-slate-900 border border-slate-800 rounded-lg px-3.5 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-indigo-500"
                  />
                  <button
                    type="submit"
                    className="px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-xs font-medium transition"
                  >
                    Add Task
                  </button>
                </form>
              </div>

              {/* Task Cards */}
              <div className="grid gap-3">
                {tasks.map((task) => (
                  <div
                    key={task.id}
                    className="bg-slate-900/60 border border-slate-800 rounded-xl p-4 flex items-center justify-between hover:border-slate-700 transition"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-400">
                          #{task.id}
                        </span>
                        <h3 className="text-sm font-semibold text-slate-200">{task.title}</h3>
                        <span className="text-[11px] font-mono text-indigo-400 bg-indigo-950/60 border border-indigo-800 px-2 py-0.5 rounded-full">
                          v{task.version ?? 0}
                        </span>
                      </div>
                      <div className="flex items-center gap-3 text-[11px] text-slate-500">
                        <span className="flex items-center gap-1">
                          <Clock className="w-3 h-3" /> Status: {task.status || "TODO"}
                        </span>
                        {task.dependencies?.length > 0 && (
                          <span className="flex items-center gap-1 text-amber-400/80">
                            <Link2 className="w-3 h-3" /> Depends on: [{task.dependencies.join(", ")}]
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Status Mutation Buttons */}
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => handleUpdateStatus(task, "IN_PROGRESS")}
                        className="px-3 py-1.5 rounded-md text-xs bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition"
                      >
                        Start
                      </button>
                      <button
                        onClick={() => handleUpdateStatus(task, "DONE")}
                        className="px-3 py-1.5 rounded-md text-xs bg-emerald-950/40 hover:bg-emerald-900/50 text-emerald-300 border border-emerald-800/80 flex items-center gap-1.5 transition"
                      >
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        <span>Complete</span>
                      </button>
                    </div>
                  </div>
                ))}

                {tasks.length === 0 && (
                  <div className="text-center py-16 border border-dashed border-slate-800 rounded-xl">
                    <p className="text-xs text-slate-500">No tasks in this project. Create one above.</p>
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="h-full flex items-center justify-center text-slate-500 text-xs">
              Select or create a project on the left to begin inspecting tasks.
            </div>
          )}
        </main>
      </div>
    </div>
  );
}