import React, { useState, useEffect } from 'react';
import { useAuth } from '../../hooks/useAuth';
import { User, Mail, Calendar, Shield, Save, LogOut, Camera, Info } from 'lucide-react';

export function ProfilePage() {
  const { user, token, logout, refreshProfile } = useAuth();
  const [profileData, setProfileData] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [bio, setBio] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');

  useEffect(() => {
    async function loadProfile() {
      if (!token) return;
      try {
        const res = await fetch("https://ais-dev-ex6yxewfd4hwymxojrmgxr-291037164760.europe-west3.run.app/api/user/profile", {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        if (res.ok) {
          const data = await res.json();
          setProfileData(data);
          setBio(data.bio || '');
          setAvatarUrl(data.avatar_url || '');
        }
      } catch (e) {
        console.error(e);
      } finally {
        setIsLoading(false);
      }
    }
    loadProfile();
  }, [token]);

  const handleSave = async () => {
    if (!token) return;
    setIsSaving(true);
    try {
      const res = await fetch("https://ais-dev-ex6yxewfd4hwymxojrmgxr-291037164760.europe-west3.run.app/api/user/profile", {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ bio, avatar_url: avatarUrl })
      });
      if (res.ok) {
        await refreshProfile();
        // Show success
      }
    } catch (e) {
      console.error(e);
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex-1 flex items-center justify-center bg-[#050810]">
        <div className="animate-spin w-8 h-8 border-2 border-cyan-500 border-t-transparent rounded-full"></div>
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto bg-[#050810] p-8 pb-24 selection:bg-cyan-500/30">
      <div className="max-w-3xl mx-auto">
        <header className="mb-12">
          <h1 className="text-4xl font-black text-white mb-2 tracking-tight">Správa Profilu</h1>
          <p className="text-slate-400 font-mono text-sm">Konfigurace vašeho uživatelského uzlu v systému O.M.N.I.S.</p>
        </header>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {/* Avatar & Basic Info */}
          <div className="md:col-span-1 space-y-6">
            <div className="bg-[#090f1d] border border-slate-800 rounded-2xl p-6 text-center shadow-xl">
              <div className="relative inline-block mb-4">
                <div className="w-32 h-32 rounded-full bg-gradient-to-br from-cyan-500 to-violet-600 flex items-center justify-center text-black overflow-hidden border-4 border-[#050810]">
                  {avatarUrl ? (
                    <img src={avatarUrl} alt="Avatar" className="w-full h-full object-cover" />
                  ) : (
                    <User className="w-16 h-16" />
                  )}
                </div>
                <button className="absolute bottom-1 right-1 w-8 h-8 bg-slate-800 rounded-full border border-slate-700 flex items-center justify-center text-cyan-400 hover:text-cyan-300 transition-colors">
                  <Camera className="w-4 h-4" />
                </button>
              </div>
              <h2 className="text-xl font-bold text-white mb-1">{user?.username}</h2>
              <p className="text-cyan-500 text-xs font-mono font-bold uppercase tracking-widest mb-4">{user?.role}</p>
              
              <div className="flex justify-center gap-2">
                <span className="px-3 py-1 bg-cyan-500/10 border border-cyan-500/20 text-cyan-500 rounded-full text-[10px] font-bold">NODE ACTIVE</span>
                <span className="px-3 py-1 bg-emerald-500/10 border border-emerald-500/20 text-emerald-500 rounded-full text-[10px] font-bold">SECURE</span>
              </div>
            </div>

            <button 
              onClick={logout}
              className="w-full py-3 px-4 bg-red-500/10 border border-red-500/20 text-red-500 rounded-xl font-bold text-sm flex items-center justify-center gap-2 hover:bg-red-500/20 transition-all"
            >
              <LogOut className="w-4 h-4" /> ODHLÁSIT SE
            </button>
          </div>

          {/* Detailed Info & Edit Form */}
          <div className="md:col-span-2 space-y-6">
            <div className="bg-[#090f1d] border border-slate-800 rounded-2xl p-8 shadow-xl relative overflow-hidden">
               <div className="absolute top-0 right-0 p-4 opacity-10"><Shield className="w-24 h-24 text-cyan-500" /></div>
               
               <h3 className="text-lg font-bold text-white mb-8 flex items-center gap-2">
                <Info className="w-4 h-4 text-cyan-500" /> Systémové Informace
               </h3>

               <div className="space-y-6">
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-[0.2em] mb-2">E-mailová Adresa</label>
                      <div className="flex items-center gap-3 text-slate-300 bg-[#050810] p-3 rounded-xl border border-slate-800/50">
                        <Mail className="w-4 h-4 text-slate-500" />
                        <span className="text-sm">{user?.email || 'Nespecifikováno'}</span>
                      </div>
                    </div>
                    <div>
                      <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-[0.2em] mb-2">Datum Registrace</label>
                      <div className="flex items-center gap-3 text-slate-300 bg-[#050810] p-3 rounded-xl border border-slate-800/50">
                        <Calendar className="w-4 h-4 text-slate-500" />
                        <span className="text-sm">{profileData?.created_at ? new Date(profileData.created_at).toLocaleDateString('cs-CZ') : '-'}</span>
                      </div>
                    </div>
                  </div>

                  <div>
                    <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-[0.2em] mb-2 ml-1">URL Profilové Fotky</label>
                    <input
                      type="text"
                      value={avatarUrl}
                      onChange={(e) => setAvatarUrl(e.target.value)}
                      className="w-full bg-[#050810] border border-slate-800 rounded-xl py-3 px-4 text-white text-sm focus:outline-none focus:border-cyan-500 transition-colors placeholder:text-slate-700 font-mono"
                      placeholder="https://..."
                    />
                  </div>

                  <div>
                    <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-[0.2em] mb-2 ml-1">Osobní Biografie / Poznámky</label>
                    <textarea
                      value={bio}
                      onChange={(e) => setBio(e.target.value)}
                      rows={4}
                      className="w-full bg-[#050810] border border-slate-800 rounded-xl py-3 px-4 text-white text-sm focus:outline-none focus:border-cyan-500 transition-colors placeholder:text-slate-700 leading-relaxed"
                      placeholder="Zde můžete uvést své preference nebo zaměření pro kognitivní syntézu..."
                    ></textarea>
                  </div>

                  <div className="pt-4 border-t border-slate-800/50 flex justify-end">
                    <button
                      onClick={handleSave}
                      disabled={isSaving}
                      className="bg-cyan-600 hover:bg-cyan-500 text-black font-bold py-2.5 px-8 rounded-xl transition-all flex items-center gap-2 group disabled:opacity-50"
                    >
                      {isSaving ? 'Ukládám...' : (
                        <>
                          <Save className="w-4 h-4" /> ULOŽIT ZMĚNY
                        </>
                      )}
                    </button>
                  </div>
               </div>
            </div>
            
            <div className="bg-cyan-500/5 border border-cyan-500/10 rounded-xl p-6 flex gap-4 items-start">
               <Shield className="w-6 h-6 text-cyan-500 shrink-0 mt-0.5" />
               <div>
                  <h4 className="text-cyan-400 font-bold text-sm mb-1 uppercase tracking-wider">Bezpečnostní Invarianty</h4>
                  <p className="text-slate-400 text-xs leading-relaxed font-mono">
                    Vaše data jsou šifrována a synchronizována s centrálním uzlem O.M.N.I.S. Přístup je chráněn skrze JWT tokenizaci s omezenou platností.
                  </p>
               </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
