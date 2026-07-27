/**
 * The .xlsx export, built with LIVE FORMULAS rather than baked-in numbers.
 *
 * That is the whole point of this file. A coordinator who receives the workbook needs to be
 * able to correct one cell — a unit price, a payment that arrived late — and watch the
 * charges, the totals and the outstanding balance move with it. A spreadsheet full of
 * constants is a screenshot with extra steps, and it goes stale the moment anyone questions
 * a figure.
 *
 * How the three sheets relate:
 *
 *   Detall de càrrecs   one row per (booking, concept). Import = Unitats × Preu unitari.
 *   Apunts              one row per ledger entry in the period.
 *   Resum               one row per volunteer, whose money columns are SUMIF/SUMIFS over the
 *                       two detail sheets, and whose hour total is a SUM over its own row.
 *
 * So every derived number in the workbook is a formula over cells that are also in the
 * workbook. Nothing is a dead constant except the raw inputs and the carry-over balance,
 * which by definition comes from outside the exported period.
 */

import ExcelJS from 'exceljs'

import { areaLabel, AREA_GENERAL, ITEM_LABEL, VOLUNTEER_TYPE_LABEL } from './contract.ts'
import { minutesToHours, type Period } from './dates.ts'
import { LEDGER_KIND_LABEL, LEDGER_METHOD_LABEL } from './ledger.ts'
import { volunteerDetail, type CoordinationTable } from './query/coordination.ts'

const MONEY = '#,##0.00\\ "€"'
const HOURS = '0.00'
const HEADER_FILL: ExcelJS.Fill = {
  type: 'pattern',
  pattern: 'solid',
  fgColor: { argb: 'FF2B2E66' },
}

/** Excel column letter for a 1-based index: 1 -> A, 27 -> AA. */
export function columnLetter(index: number): string {
  let n = index
  let out = ''
  while (n > 0) {
    const rem = (n - 1) % 26
    out = String.fromCharCode(65 + rem) + out
    n = Math.floor((n - 1) / 26)
  }
  return out
}

export interface ExportOptions {
  table: CoordinationTable
  period: Period
  /** Whose export this is, stamped on the cover row so a stale file is identifiable. */
  actor: string
  generatedAt: Date
  areaFilterLabel: string | null
}

export async function buildWorkbook(options: ExportOptions): Promise<ArrayBuffer> {
  const { table } = options

  const workbook = new ExcelJS.Workbook()
  workbook.creator = 'Panell de voluntariat · Casa Virupa'
  workbook.created = options.generatedAt

  // Resum is added first so it is the sheet that opens; the formulas in it reference the
  // detail sheets by name, which does not require them to exist yet.
  buildSummarySheet(workbook, options, table.showPayments)
  if (table.showPayments) {
    buildChargesSheet(workbook, options)
    buildEntriesSheet(workbook, options)
  }

  return workbook.xlsx.writeBuffer()
}

// --- Resum -------------------------------------------------------------------

function buildSummarySheet(
  workbook: ExcelJS.Workbook,
  { table, period, actor, generatedAt, areaFilterLabel }: ExportOptions,
  withMoney: boolean,
): void {
  const sheet = workbook.addWorksheet('Resum', {
    views: [{ state: 'frozen', xSplit: 1, ySplit: 4 }],
  })

  sheet.getCell('A1').value = `Voluntariat Casa Virupa · ${period.label}`
  sheet.getCell('A1').font = { bold: true, size: 14, color: { argb: 'FF2B2E66' } }
  sheet.getCell('A2').value =
    `Període ${period.from} → ${period.to} (fi exclosa)` +
    (areaFilterLabel ? ` · àrea: ${areaFilterLabel}` : '') +
    ` · generat per ${actor} el ${generatedAt.toISOString().slice(0, 16).replace('T', ' ')}`
  sheet.getCell('A2').font = { size: 9, color: { argb: 'FF64748B' } }
  sheet.getCell('A3').value =
    'Hores reals (fi − inici de cada torn). Els imports són fórmules sobre els fulls de detall: ' +
    'canvia un preu unitari al full «Detall de càrrecs» i tot es recalcula. Un objectiu marcat ' +
    'amb ~ està proratejat des d’un període diferent.'
  sheet.getCell('A3').font = { size: 9, italic: true, color: { argb: 'FF64748B' } }

  // --- header row -----------------------------------------------------------
  const header: string[] = ['Voluntari', 'Tipus', 'Soci']
  const areaFirstCol = header.length + 1

  for (const area of table.areaColumns) {
    const label = area === AREA_GENERAL ? 'Vol. general' : areaLabel(area)
    header.push(`${label} (h)`, `${label} obj.`, `${label} %`)
  }

  const totalCol = header.length + 1
  header.push('Total hores', 'Compromís total', 'Total %')

  let mealCol = 0
  if (withMoney) {
    mealCol = header.length + 1
    header.push(
      'Dinars',
      'Sopars',
      'Pernoctes',
      'Càrrecs (€)',
      'Saldo anterior (€)',
      'Pagaments i ajustos (€)',
      'Deute total (€)',
    )
  }

  const headerRow = sheet.getRow(4)
  headerRow.values = header
  headerRow.eachCell((cell) => {
    cell.font = { bold: true, size: 9, color: { argb: 'FFFFFFFF' } }
    cell.fill = HEADER_FILL
    cell.alignment = { vertical: 'middle', wrapText: true }
  })
  headerRow.height = 30

  // --- one row per volunteer ------------------------------------------------
  const firstDataRow = 5
  let r = firstDataRow

  for (const row of table.rows) {
    const excelRow = sheet.getRow(r)

    excelRow.getCell(1).value = row.name
    excelRow.getCell(2).value = row.volunteerType
      ? VOLUNTEER_TYPE_LABEL[row.volunteerType as 'mitra' | 'habitual']
      : ''
    excelRow.getCell(3).value = row.isMember ? 'Sí' : 'No'

    let col = areaFirstCol
    for (const area of table.areaColumns) {
      const progress = row.progressByArea.get(area)
      const doneCell = excelRow.getCell(col)
      const targetCell = excelRow.getCell(col + 1)
      const pctCell = excelRow.getCell(col + 2)

      doneCell.value = minutesToHours(row.minutesByArea.get(area) ?? 0)
      doneCell.numFmt = HOURS

      if (progress?.commitment) {
        targetCell.value = minutesToHours(progress.commitment.targetMinutes)
        targetCell.numFmt = HOURS
        // Prorated targets are annotated rather than silently presented as exact.
        if (progress.commitment.scaled) {
          targetCell.note = `Proratejat des de ${
            progress.commitment.nativePeriodKind === 'quarter' ? 'un trimestre' : 'un mes'
          } (${minutesToHours(progress.commitment.nativeTargetMinutes)} h). El termini real és més tard.`
          targetCell.font = { italic: true }
        }
      }

      pctCell.value = ratioFormula(columnLetter(col), columnLetter(col + 1), r)
      pctCell.numFmt = '0 %'
      col += 3
    }

    // Total hours is a SUM over this row's area columns, not a precomputed number, so
    // correcting one area corrects the total.
    const totalCell = excelRow.getCell(totalCol)
    totalCell.value =
      table.areaColumns.length === 0
        ? 0
        : {
            formula: table.areaColumns
              .map((_, i) => `${columnLetter(areaFirstCol + i * 3)}${r}`)
              .join('+'),
          }
    totalCell.numFmt = HOURS
    totalCell.font = { bold: true }

    const totalTarget = excelRow.getCell(totalCol + 1)
    if (row.totalProgress.commitment) {
      totalTarget.value = minutesToHours(row.totalProgress.commitment.targetMinutes)
      totalTarget.numFmt = HOURS
      if (row.totalProgress.commitment.scaled) {
        totalTarget.font = { italic: true }
        totalTarget.note = `Proratejat des de ${
          row.totalProgress.commitment.nativePeriodKind === 'quarter' ? 'un trimestre' : 'un mes'
        } (${minutesToHours(row.totalProgress.commitment.nativeTargetMinutes)} h).`
      }
    }
    const totalPct = excelRow.getCell(totalCol + 2)
    totalPct.value = ratioFormula(columnLetter(totalCol), columnLetter(totalCol + 1), r)
    totalPct.numFmt = '0 %'

    if (withMoney) {
      const uid = row.uid
      const c = (offset: number) => excelRow.getCell(mealCol + offset)

      c(0).value = countFormula(uid, ITEM_LABEL.lunch)
      c(1).value = countFormula(uid, ITEM_LABEL.dinner)
      c(2).value = countFormula(uid, ITEM_LABEL.sleep)

      const chargesCell = c(3)
      chargesCell.value = { formula: `SUMIF('Detall de càrrecs'!$B:$B,"${uid}",'Detall de càrrecs'!$G:$G)` }
      chargesCell.numFmt = MONEY

      // The only figure that cannot be a formula: it comes from outside the exported period.
      const carryCell = c(4)
      carryCell.value = row.balance.carryInCents / 100
      carryCell.numFmt = MONEY
      carryCell.note =
        'Saldo acumulat abans de l’inici del període. No es pot recalcular des d’aquest full ' +
        'perquè els càrrecs i els pagaments anteriors no s’hi exporten.'

      const creditsCell = c(5)
      creditsCell.value = { formula: `SUMIF(Apunts!$B:$B,"${uid}",Apunts!$E:$E)` }
      creditsCell.numFmt = MONEY

      const owedCell = c(6)
      const L = (offset: number) => columnLetter(mealCol + offset)
      owedCell.value = { formula: `${L(3)}${r}+${L(4)}${r}-${L(5)}${r}` }
      owedCell.numFmt = MONEY
      owedCell.font = { bold: true }
    }

    r++
  }

  // --- totals row -----------------------------------------------------------
  const lastDataRow = r - 1
  if (lastDataRow >= firstDataRow) {
    const totals = sheet.getRow(r + 1)
    totals.getCell(1).value = 'TOTAL'
    totals.getCell(1).font = { bold: true }

    const sumColumns: number[] = []
    for (let i = 0; i < table.areaColumns.length; i++) sumColumns.push(areaFirstCol + i * 3)
    sumColumns.push(totalCol, totalCol + 1)
    if (withMoney) {
      for (let offset = 0; offset <= 6; offset++) {
        if (offset !== 6) sumColumns.push(mealCol + offset)
      }
      sumColumns.push(mealCol + 6)
    }

    for (const col of sumColumns) {
      const letter = columnLetter(col)
      const cell = totals.getCell(col)
      cell.value = { formula: `SUM(${letter}${firstDataRow}:${letter}${lastDataRow})` }
      cell.numFmt = col >= mealCol && withMoney && col >= mealCol + 3 ? MONEY : HOURS
      cell.font = { bold: true }
    }
    // Counts are whole numbers, not hours.
    if (withMoney) {
      for (let offset = 0; offset <= 2; offset++) totals.getCell(mealCol + offset).numFmt = '0'
    }
  }

  sheet.getColumn(1).width = 26
  sheet.getColumn(2).width = 10
  sheet.getColumn(3).width = 6
  for (let col = areaFirstCol; col <= header.length; col++) {
    sheet.getColumn(col).width = 11
  }
  sheet.autoFilter = {
    from: { row: 4, column: 1 },
    to: { row: Math.max(4, lastDataRow), column: header.length },
  }
}

/** `=IF(target=0,"",done/target)` — a missing commitment must read blank, not #DIV/0!. */
function ratioFormula(doneCol: string, targetCol: string, row: number): ExcelJS.CellFormulaValue {
  return {
    formula: `IF(N(${targetCol}${row})=0,"",${doneCol}${row}/${targetCol}${row})`,
    result: undefined,
  } as ExcelJS.CellFormulaValue
}

function countFormula(uid: string, itemLabel: string): ExcelJS.CellFormulaValue {
  return {
    formula:
      `SUMIFS('Detall de càrrecs'!$E:$E,'Detall de càrrecs'!$B:$B,"${uid}",` +
      `'Detall de càrrecs'!$D:$D,"${itemLabel}")`,
    result: undefined,
  } as ExcelJS.CellFormulaValue
}

// --- Detall de càrrecs -------------------------------------------------------

function buildChargesSheet(workbook: ExcelJS.Workbook, { table, period }: ExportOptions) {
  const sheet = workbook.addWorksheet('Detall de càrrecs', {
    views: [{ state: 'frozen', ySplit: 1 }],
  })

  sheet.columns = [
    { header: 'Voluntari', key: 'name', width: 26 },
    { header: 'uid', key: 'uid', width: 22 },
    { header: 'Data', key: 'date', width: 12 },
    { header: 'Concepte', key: 'item', width: 12 },
    { header: 'Unitats', key: 'qty', width: 9 },
    { header: 'Preu unitari (€)', key: 'unit', width: 15 },
    { header: 'Import (€)', key: 'amount', width: 13 },
    { header: 'Congelat', key: 'locked', width: 10 },
  ]
  styleHeader(sheet)

  let r = 2
  for (const row of table.rows) {
    const detail = volunteerDetail(row.uid, period)
    for (const charge of detail.charges) {
      const excelRow = sheet.getRow(r)
      excelRow.getCell(1).value = row.name
      excelRow.getCell(2).value = row.uid
      excelRow.getCell(3).value = charge.serviceDate
      excelRow.getCell(4).value = ITEM_LABEL[charge.item as keyof typeof ITEM_LABEL] ?? charge.item
      excelRow.getCell(5).value = charge.qty
      const unit = excelRow.getCell(6)
      unit.value = charge.unitPriceCents / 100
      unit.numFmt = MONEY
      if (!charge.hasPrice) {
        unit.note = 'Cap regla de preu aplicable en aquesta data: compta 0 €.'
        unit.font = { color: { argb: 'FFDC2626' } }
      }
      // The live bit: editing E or F moves G, and G feeds the Resum sheet.
      const amount = excelRow.getCell(7)
      amount.value = { formula: `E${r}*F${r}` }
      amount.numFmt = MONEY
      excelRow.getCell(8).value = charge.locked ? 'Sí' : ''
      r++
    }
  }

  if (r > 2) {
    const totals = sheet.getRow(r + 1)
    totals.getCell(4).value = 'TOTAL'
    totals.getCell(4).font = { bold: true }
    totals.getCell(5).value = { formula: `SUM(E2:E${r - 1})` }
    const total = totals.getCell(7)
    total.value = { formula: `SUM(G2:G${r - 1})` }
    total.numFmt = MONEY
    total.font = { bold: true }
  }

  sheet.autoFilter = { from: 'A1', to: { row: 1, column: 8 } }
  return sheet
}

// --- Apunts ------------------------------------------------------------------

function buildEntriesSheet(workbook: ExcelJS.Workbook, { table, period }: ExportOptions) {
  const sheet = workbook.addWorksheet('Apunts', { views: [{ state: 'frozen', ySplit: 1 }] })

  sheet.columns = [
    { header: 'Voluntari', key: 'name', width: 26 },
    { header: 'uid', key: 'uid', width: 22 },
    { header: 'Data efectiva', key: 'date', width: 14 },
    { header: 'Tipus', key: 'kind', width: 14 },
    { header: 'Import (€)', key: 'amount', width: 13 },
    { header: 'Mètode', key: 'method', width: 15 },
    { header: 'Nota', key: 'note', width: 34 },
    { header: 'Registrat per', key: 'by', width: 26 },
  ]
  styleHeader(sheet)

  let r = 2
  for (const row of table.rows) {
    const detail = volunteerDetail(row.uid, period)
    // Only the period's own entries: the Resum SUMIF over this sheet must match the
    // period's credits exactly, so an entry from another month cannot leak in.
    for (const entry of detail.ledger.filter(
      (e) => e.effectiveDate >= period.from && e.effectiveDate < period.to,
    )) {
      const excelRow = sheet.getRow(r)
      excelRow.getCell(1).value = row.name
      excelRow.getCell(2).value = row.uid
      excelRow.getCell(3).value = entry.effectiveDate
      excelRow.getCell(4).value = LEDGER_KIND_LABEL[entry.kind] ?? entry.kind
      const amount = excelRow.getCell(5)
      amount.value = entry.amountCents / 100
      amount.numFmt = MONEY
      excelRow.getCell(6).value = entry.method
        ? (LEDGER_METHOD_LABEL[entry.method] ?? entry.method)
        : ''
      excelRow.getCell(7).value =
        (entry.note ?? '') +
        (entry.voidsId !== null ? ` (anul·la l’apunt #${entry.voidsId})` : '') +
        (entry.voided ? ' (anul·lat)' : '')
      excelRow.getCell(8).value = entry.createdBy
      r++
    }
  }

  if (r > 2) {
    const totals = sheet.getRow(r + 1)
    totals.getCell(4).value = 'TOTAL'
    totals.getCell(4).font = { bold: true }
    const total = totals.getCell(5)
    total.value = { formula: `SUM(E2:E${r - 1})` }
    total.numFmt = MONEY
    total.font = { bold: true }
  }

  sheet.getCell(`A${r + 3}`).value =
    'Un apunt positiu és un crèdit: redueix el que deu el voluntari. Els apunts no s’editen ' +
    'mai — anul·lar-ne un afegeix el seu contrari, i per tant la suma d’aquesta columna ja és neta.'
  sheet.getCell(`A${r + 3}`).font = { size: 9, italic: true, color: { argb: 'FF64748B' } }

  sheet.autoFilter = { from: 'A1', to: { row: 1, column: 8 } }
  return sheet
}

function styleHeader(sheet: ExcelJS.Worksheet): void {
  const header = sheet.getRow(1)
  header.eachCell((cell) => {
    cell.font = { bold: true, size: 9, color: { argb: 'FFFFFFFF' } }
    cell.fill = HEADER_FILL
  })
}
