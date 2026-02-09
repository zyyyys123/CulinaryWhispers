/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        serif: ['Playfair Display', 'Georgia', 'serif'],
        mono: ['Fira Code', 'monospace'],
      },
      colors: {
        primary: {
          DEFAULT: '#D4AF37',
          50: '#F9F6E8',
        },
        dark: {
          bg: '#121212',
          surface: '#1E1E1E',
        }
      }
    },
  },
  plugins: [],
}
