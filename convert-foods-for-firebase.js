// Node.js script to convert gym-foods-500-final.json (array) to foods-for-firebase.json (object)
const fs = require('fs');
const input = JSON.parse(fs.readFileSync('gym-foods-500-final.json', 'utf8'));
const foodsObj = {};
input.forEach((food, idx) => {
  // Use a unique key for each food
  const key = food.name.replace(/[^a-zA-Z0-9]/g, '_').toLowerCase() + '_' + idx;
  foodsObj[key] = food;
});
fs.writeFileSync('foods-for-firebase.json', JSON.stringify({ foods: foodsObj }, null, 2));
console.log('foods-for-firebase.json created!');

