/**
 * Balance arithmetic. Pure and integer-only — see the note on float euros in AGENTS.md:
 * `balance === 0` is the comparison that decides whether someone has paid, and in floats
 * it is intermittently false.
 *
 * The model, restated because the sign convention trips everyone up once:
 *
 *   charges  positive cents, DERIVED from the mirror × the dated price table
 *   ledger   POSITIVE = CREDIT — a payment, a discount, a write-off. Negative = a debit
 *            (a charge the mirror cannot know about, e.g. a broken tool).
 *   owed     charges − Σ ledger
 *
 * A period's own figures never include what came before. `carryIn` is everything up to
 * the period start, carried as a separate line so `owedTotal = carryIn + owedPeriod` and
 * the two are always reconcilable. That matters for write-back: only `owedPeriod`-style
 * month charges may ever be written to Firestore, never a total that includes carry-over,
 * because the app does `amount += delta` and a carried balance would compound.
 */

/** 'positive = credit' entries. */
export type LedgerKind = 'payment' | 'adjustment' | 'opening_balance' | 'write_off'

export const LEDGER_KIND_LABEL: Record<string, string> = {
  payment: 'Pagament',
  adjustment: 'Ajust',
  opening_balance: 'Saldo inicial',
  write_off: 'Condonació',
}

export const LEDGER_METHOD_LABEL: Record<string, string> = {
  cash: 'Efectiu',
  transfer: 'Transferència',
  app: "Des de l'app",
}

export interface Balance {
  /** Derived charges inside the period. */
  chargesCents: number
  /** Σ ledger inside the period (positive = credit). */
  creditsCents: number
  /** Of those credits, the part that is a payment rather than an adjustment. */
  paymentsCents: number
  /** Everything before the period start, netted. Positive = they arrived owing. */
  carryInCents: number
  /** charges − credits, inside the period only. */
  owedPeriodCents: number
  /** carryIn + owedPeriod — what the volunteer actually owes right now. */
  owedTotalCents: number
}

export function balanceOf(input: {
  chargesCents: number
  creditsCents: number
  paymentsCents: number
  carryInCents: number
}): Balance {
  const owedPeriodCents = input.chargesCents - input.creditsCents
  return {
    chargesCents: input.chargesCents,
    creditsCents: input.creditsCents,
    paymentsCents: input.paymentsCents,
    carryInCents: input.carryInCents,
    owedPeriodCents,
    owedTotalCents: input.carryInCents + owedPeriodCents,
  }
}

export const ZERO_BALANCE: Balance = {
  chargesCents: 0,
  creditsCents: 0,
  paymentsCents: 0,
  carryInCents: 0,
  owedPeriodCents: 0,
  owedTotalCents: 0,
}

export type SettlementState = 'settled' | 'owing' | 'credit' | 'nothing'

/**
 * Anything within a cent is settled — not because of float error (these are integers) but
 * because a 1-cent residue from a rounded proration is not a debt anybody chases.
 */
export function settlementOf(b: Balance): SettlementState {
  if (b.chargesCents === 0 && b.creditsCents === 0 && b.carryInCents === 0) return 'nothing'
  if (Math.abs(b.owedTotalCents) <= 1) return 'settled'
  return b.owedTotalCents > 0 ? 'owing' : 'credit'
}

export const SETTLEMENT_LABEL: Record<SettlementState, string> = {
  settled: 'Pagat',
  owing: 'Pendent',
  credit: 'A favor',
  nothing: '—',
}

export const SETTLEMENT_CLASS: Record<SettlementState, string> = {
  settled: 'bg-ok/10 text-ok ring-ok/20',
  owing: 'bg-bad/10 text-bad ring-bad/20',
  credit: 'bg-brand-50 text-brand-700 ring-brand-200',
  nothing: 'bg-slate-50 text-slate-400 ring-slate-200',
}

/**
 * An amount that is an adjustment TO another figure, always carrying its sign: '−50,00 €'
 * for a payment, '+5,00 €' for an extra charge. Without the explicit plus, a negated credit
 * reads as if the volunteer had paid it.
 */
export function formatSignedCents(cents: number, format: (c: number) => string): string {
  return cents > 0 ? `+${format(cents)}` : format(cents)
}

/**
 * Parses a euro amount typed by a human into integer cents. Accepts the Catalan comma as
 * well as a dot, and rejects rather than truncates anything else — a payment silently
 * becoming 0 € is worse than a form error.
 */
export function parseEurosToCents(input: string): number | null {
  const cleaned = input.trim().replace(/\s|€/g, '').replace(',', '.')
  if (cleaned === '' || !/^-?\d+(\.\d{1,2})?$/.test(cleaned)) return null
  return Math.round(Number(cleaned) * 100)
}
