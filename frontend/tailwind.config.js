/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
    "./frontend/index.html",
    "./frontend/src/**/*.{js,ts,jsx,tsx}"
  ],
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        omnis: {
          bg: "#080c14",
          panel: "#0f172a",
          border: "#1e293b",
          primary: "#00E5FF",
          accent: "#7C4DFF",
          warning: "#F59E0B",
          success: "#10B981",
        },
      },
    },
  },
  plugins: [],
};
