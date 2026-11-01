import React, { useState } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { Lock, User, Mail, Shield, ArrowRight, Chrome, Terminal, ChevronDown, ChevronUp, AlertCircle } from 'lucide-react';

export function AuthScreens() {
  const [isLogin, setIsLogin] = useState(true);
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [showLogs, setShowLogs] = useState(false);
  
  const { login, register, loginWithHybridGoogle, diagnosticLogs } = useAuth();

  const handleGoogleSignIn = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await loginWithHybridGoogle();
      if (!res.success) {
        setError(res.error || "Chyba při přihlášení přes Google.");
      } else {
        window.location.reload();
      }
    } catch (err: any) {
      console.error("[AuthScreens Google Error]:", err);
      setError(err?.message || "Neočekávaná chyba při přihlašování.");
    } finally {
      setIsLoading(false);
    }
  };

  const handleEmergencyAdminLogin = () => {
    localStorage.setItem('omnis_token', 'emergency_admin_token_active');
    localStorage.setItem('omnis_user_role', 'ADMIN_OPERATOR');
    localStorage.setItem('omnis_user_email', 'susetofu@gmail.com');
    window.location.reload();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsLoading(true);

    let res;
    if (isLogin) {
      res = await login(username, password);
    } else {
      res = await register(username, email, password);
    }

    if (!res.success) {
      setError(res.error || "Chyba při přihlášení");
    }
    setIsLoading(false);
  };

  return (
    <div className="min-h-screen bg-[#050810] flex items-center justify-center p-4 selection:bg-cyan-500/30">
      <div className="max-w-md w-full">
        {/* Logo/Branding */}
        <div className="flex items-center gap-4 mb-8 justify-center">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-cyan-500 to-violet-600 flex items-center justify-center text-black font-bold text-2xl shadow-[0_0_20px_rgba(6,182,212,0.3)]">
            Ω
          </div>
          <div>
            <h1 className="text-white font-black text-xl tracking-wider leading-none">O.M.N.I.S.</h1>
            <p className="text-cyan-500 font-mono text-[10px] font-bold uppercase tracking-tighter">Cognitive OS & Gateway</p>
          </div>
        </div>

        <div className="bg-[#090f1d] border border-slate-800 rounded-2xl p-8 shadow-2xl relative overflow-hidden">
          <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-cyan-500 via-violet-500 to-cyan-500 opacity-50"></div>
          
          <div className="mb-8">
            <h2 className="text-2xl font-bold text-white mb-2">{isLogin ? 'Přihlášení' : 'Nová registrace'}</h2>
            <p className="text-slate-400 text-sm font-mono">
              {isLogin ? 'Vstup do kognitivního rozhraní O.M.N.I.S.' : 'Vytvořte si nový přístupový uzel.'}
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest mb-1.5 ml-1">Uživatelské jméno</label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="w-full bg-[#050810] border border-slate-800 rounded-xl py-3 pl-10 pr-4 text-white focus:outline-none focus:border-cyan-500 transition-colors placeholder:text-slate-700 font-mono text-sm"
                  placeholder="admin / operator"
                  required
                />
              </div>
            </div>

            {!isLogin && (
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest mb-1.5 ml-1">E-mailová adresa</label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full bg-[#050810] border border-slate-800 rounded-xl py-3 pl-10 pr-4 text-white focus:outline-none focus:border-violet-500 transition-colors placeholder:text-slate-700 font-mono text-sm"
                    placeholder="name@example.com"
                    required
                  />
                </div>
              </div>
            )}

            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest mb-1.5 ml-1">Heslo</label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full bg-[#050810] border border-slate-800 rounded-xl py-3 pl-10 pr-4 text-white focus:outline-none focus:border-cyan-500 transition-colors placeholder:text-slate-700 font-mono text-sm"
                  placeholder="••••••••"
                  required
                />
              </div>
            </div>

            {error && (
              <div className="bg-red-500/10 border border-red-500/20 text-red-400 text-xs py-2 px-3 rounded-lg font-mono flex items-start gap-2">
                <AlertCircle className="w-4 h-4 text-red-400 mt-0.5 flex-shrink-0" />
                <div className="flex-1">
                  <p>{error}</p>
                  {error.includes("16") && (
                    <p className="text-[10px] text-amber-400 mt-1">
                      Detekováno selhání Google reauth [16]. Můžete využít přímý nouzový vstup níže.
                    </p>
                  )}
                </div>
              </div>
            )}

            <button
              type="submit"
              disabled={isLoading}
              className={`w-full ${isLogin ? 'bg-cyan-600 hover:bg-cyan-500' : 'bg-violet-600 hover:bg-violet-500'} text-black font-bold py-3 rounded-xl transition-all flex items-center justify-center gap-2 group disabled:opacity-50 text-sm`}
            >
              {isLoading ? 'Zpracovávám...' : (isLogin ? 'PŘIHLÁSIT SE' : 'ZAREGISTROVAT SE')}
              {!isLoading && <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />}
            </button>
          </form>

          <div className="mt-6 flex flex-col gap-3">
            <div className="relative flex items-center justify-center">
              <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-slate-800"></div></div>
              <span className="relative px-3 bg-[#090f1d] text-[10px] text-slate-500 font-bold uppercase tracking-widest">Nebo pokračujte přes</span>
            </div>
            
            <button 
              onClick={handleGoogleSignIn}
              disabled={isLoading}
              className="w-full bg-white text-black font-bold py-2.5 rounded-xl flex items-center justify-center gap-2 text-sm hover:bg-slate-100 transition-colors disabled:opacity-50 shadow-md"
            >
              <Chrome className="w-4 h-4" /> {isLoading ? 'Autentizace...' : 'Sign in with Google (Hybrid)'}
            </button>

            {/* Nouzové přihlášení správce */}
            <button
              onClick={handleEmergencyAdminLogin}
              className="w-full py-2 px-3 rounded-xl border border-cyan-500/30 bg-cyan-950/20 hover:bg-cyan-950/40 text-cyan-400 text-xs font-mono font-bold flex items-center justify-center gap-2 transition-all"
            >
              <Shield className="w-3.5 h-3.5 text-cyan-400" />
              <span>PŘÍMÝ VSTUP: ADMIN OPERÁTOR (BYPASS)</span>
            </button>
          </div>

          <div className="mt-6 text-center">
            <button
              onClick={() => setIsLogin(!isLogin)}
              className="text-slate-400 text-xs hover:text-cyan-400 transition-colors font-mono"
            >
              {isLogin ? 'Nemáte účet? Zaregistrujte se zde' : 'Už máte účet? Přihlaste se'}
            </button>
          </div>

          {/* Diagnostický logovací panel */}
          <div className="mt-6 pt-4 border-t border-slate-800/80">
            <button
              type="button"
              onClick={() => setShowLogs(!showLogs)}
              className="w-full flex items-center justify-between text-slate-500 hover:text-slate-300 text-[11px] font-mono transition-colors"
            >
              <span className="flex items-center gap-1.5">
                <Terminal className="w-3.5 h-3.5 text-cyan-400" />
                <span>DIAGNOSTIKA AUTH STAVU ({diagnosticLogs.length})</span>
              </span>
              {showLogs ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
            </button>

            {showLogs && (
              <div className="mt-2.5 bg-[#050810] p-2.5 rounded-lg border border-slate-800 max-h-36 overflow-y-auto font-mono text-[10px] text-slate-400 space-y-1">
                {diagnosticLogs.length === 0 ? (
                  <p className="text-slate-600">Žádné diagnostické události zatím nezaznamenány.</p>
                ) : (
                  diagnosticLogs.map((log, idx) => (
                    <div key={idx} className="leading-tight break-all border-b border-slate-900/60 pb-0.5">
                      {log}
                    </div>
                  ))
                )}
              </div>
            )}
          </div>
        </div>
        
        <p className="mt-6 text-center text-slate-600 text-[10px] font-mono uppercase tracking-[0.2em]">
          End-to-End Encrypted • Neural Nexus Protection Active
        </p>
      </div>
    </div>
  );
}
