import re

with open("frontend/src/App.tsx", "r") as f:
    content = f.read()

# I will just append the missing closing tags and the Chat Input section to make it compile!
append_str = """
                <div ref={messagesEndRef} />
              </div>
              
              {/* Chat Input */}
              <div className="p-4 sm:p-6 bg-slate-950/50 border-t border-slate-800/80">
                <form 
                  onSubmit={(e) => { 
                    e.preventDefault(); 
                    if (!isLoading && inputQuery.trim()) {
                      handleSendQuery(inputQuery);
                    }
                  }} 
                  className="flex gap-3 max-w-4xl mx-auto"
                >
                  <input 
                    type="text" 
                    value={inputQuery} 
                    onChange={e => setInputQuery(e.target.value)} 
                    placeholder="Zpráva pro O.M.N.I.S..." 
                    className="flex-1 bg-slate-900 border border-slate-700/60 rounded-xl px-4 py-3 text-sm text-slate-200 focus:outline-none focus:border-[#00F0FF]/50"
                  />
                  <button 
                    type="submit" 
                    disabled={isLoading || !inputQuery.trim()} 
                    className="bg-gradient-to-r from-[#00F0FF] to-blue-600 hover:opacity-90 text-slate-950 px-6 py-3 rounded-xl font-bold font-mono transition-opacity disabled:opacity-50"
                  >
                    Odeslat
                  </button>
                </form>
              </div>
            </div>
          )}

          {activeTab === "dev_lab" && <DevPromptLab />}
          {activeTab === "octagon" && <OctagonDashboard />}
        </div>
      </div>
    </div>
  );
}
"""

with open("frontend/src/App.tsx", "a") as f:
    f.write(append_str)

