/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#05944F',
          hover: '#047840',
          tint: '#E6F4EE',
          ink: '#046C3A',
        },
        'on-primary': '#FFFFFF',
        'accent-black': '#141414',
        background: '#F6F6F6',
        surface: {
          DEFAULT: '#FFFFFF',
          subtle: '#F3F3F3',
          elevated: '#F3F3F3',
        },
        border: {
          DEFAULT: '#E2E2E2',
          light: '#EEEEEE',
        },
        'on-surface': '#141414',
        'text-secondary': '#545454',
        'text-muted': '#757575',
        warning: {
          DEFAULT: '#D97706',
          tint: '#FEF3C7',
          ink: '#92400E',
        },
        error: {
          DEFAULT: '#DC2626',
          tint: '#FEE2E2',
          ink: '#B91C1C',
        },
        success: {
          DEFAULT: '#05944F',
          tint: '#E6F4EE',
          ink: '#046C3A',
        },
        info: {
          DEFAULT: '#0284C7',
          tint: '#EEF2F6',
          ink: '#334155',
        },
        viewfinder: '#1C1F1E',
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
      },
      borderRadius: {
        sm: '6px',
        md: '8px',
        lg: '12px',
        xl: '16px',
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
