const defaultTheme = require('tailwindcss/defaultTheme')

module.exports = {
  // in prod look at shadow-cljs output file in dev look at runtime, which will change files that are actually compiled; postcss watch should be a whole lot faster
  content: process.env.NODE_ENV == 'production' ? ["./public/js/main.js"] : ["./src/main/**/*.cljs", "./public/js/cljs-runtime/*.js"],
  theme: {
    extend: {
      fontFamily: {
        sans: ["Inter var", ...defaultTheme.fontFamily.sans],
      },
      colors: {
        "brazz": {
          50: "#FFFAEB",
          100: "#FFF6DB",
          200: "#FFEBAD",
          300: "#FFE085",
          400: "#FFD24D",
          500: "#FFC107",
          600: "#EBB000",
          700: "#CC9900",
          800: "#A87E00",
          900: "#7A5C00",
          950: "#574100"
        }
      }
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
  ],
}