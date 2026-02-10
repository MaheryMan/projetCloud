import type { Id, Timestamp } from './common.types'

export type ReportStatus = string

export type ReportType = string

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
  niveau?: number
  createdAt: Timestamp
}

export interface Photo {
  id?: Id
  reportId: Id
  uid: Id
  imgbbUrl: string
  uploadedAt: Timestamp
}
