/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#00E676',
          hover: '#00C853',
        },
        'on-primary': '#06120B',
        background: '#0A0E0D',
        surface: {
          DEFAULT: '#121816',
          elevated: '#1A2320',
        },
        border: '#26332E',
        'on-surface': '#F2F7F5',
        'text-muted': '#94A39E',
        warning: '#FFB300',
        error: '#FF4D4F',
        success: '#00E676',
        info: '#29B6F6',
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
      },
      borderRadius: {
        sm: '6px',
        md: '10px',
        lg: '14px',
        full: '9999px',
      },
      spacing: {
        touch: '52px',
      },
      minHeight: {
        touch: '52px',
      },
      height: {
        touch: '52px',
      },
    },
  },
  plugins: [],
};
