import { CSV_COLUMNS } from '../shared/constants.js'
import { ValidationError } from '../domain/errors/ValidationError.js'
import { mapRecordToPayment } from './paymentRecordMapper.js'

function parseCsvLine(line) {
  const values = []
  let current = ''
  let inQuotes = false

  for (let i = 0; i < line.length; i += 1) {
    const char = line[i]
    const next = line[i + 1]

    if (char === '"' && inQuotes && next === '"') {
      current += '"'
      i += 1
      continue
    }

    if (char === '"') {
      inQuotes = !inQuotes
      continue
    }

    if (char === ',' && !inQuotes) {
      values.push(current.trim())
      current = ''
      continue
    }

    current += char
  }

  values.push(current.trim())
  return values
}

function parseCsvText(text) {
  const lines = text
    .replace(/^\uFEFF/, '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)

  if (lines.length === 0) {
    throw new ValidationError('CSV file is empty')
  }

  const headers = parseCsvLine(lines[0]).map((header) => header.trim())
  const missingColumns = CSV_COLUMNS.filter((column) => !headers.includes(column))

  if (missingColumns.length > 0) {
    throw new ValidationError(`Missing required columns: ${missingColumns.join(', ')}`)
  }

  return lines.slice(1).map((line, index) => {
    const values = parseCsvLine(line)
    const row = {}

    headers.forEach((header, columnIndex) => {
      row[header] = values[columnIndex] ?? ''
    })

    return {
      rowIndex: index + 2,
      raw: row,
    }
  })
}

export async function readCsvFile(file) {
  const text = await file.text()
  return parseCsvText(text)
}

export const mapRowToPayment = mapRecordToPayment
