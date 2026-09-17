const fs = require('node:fs');
fs.mkdirSync('src/main/resources/static/vendor', { recursive: true });
for (const [source, destination] of [
  ['node_modules/fullcalendar/index.global.min.js', 'fullcalendar-6.1.10.min.js'],
  ['node_modules/fullcalendar/LICENSE.md', 'fullcalendar-LICENSE.md']
]) fs.copyFileSync(source, 'src/main/resources/static/vendor/' + destination);
