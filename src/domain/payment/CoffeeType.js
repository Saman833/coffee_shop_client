export const COFFEE_TYPES = [
  'ESPRESSO',
  'DOUBLE_ESPRESSO',
  'AMERICANO',
  'LATTE',
  'CAPPUCCINO',
  'FLAT_WHITE',
  'MOCHA',
  'CORTADO',
  'MACCHIATO',
  'COLD_BREW',
]

export function isCoffeeType(value) {
  return COFFEE_TYPES.includes(value)
}
