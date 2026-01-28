import type { Id, Timestamp } from './common.types'

export type ReportStatus = 'nouveau' | 'en_cours' | 'termine'

export type ReportType =
  | 'trou'
  | 'chantier'
  | 'deviation'
  | 'autre'

export interface Report {
  id?: Id
  uid: Id
  description: string
  type: ReportType
  lat: number
  lng: number
  status: ReportStatus
  surfaceM2?: number
  budgetEstimated?: number
  companyName?: string
  photo?: string
  createdAt: Timestamp
}
