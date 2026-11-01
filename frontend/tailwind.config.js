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
          bg: "#060913",
          surface: "#0A0F1D",
          panel: "#0F172A",
          card: "#141E33",
          border: "#1E2D4A",
          primary: "#00F0FF",
          accent: "#A855F7",
          secondary: "#3B82F6",
          warning: "#F59E0B",
          success: "#10B981",
          danger: "#F43F5E",
        },
      },
    },
  },
  plugins: [],
};
