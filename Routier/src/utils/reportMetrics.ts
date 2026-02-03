import type { Report } from '@/types/report.types'

export interface ReportMetrics {
  totalReports: number
  totalSurfaceM2: number
  totalBudgetEstimated: number
  progressPercent: number
}

export function computeReportMetrics(reports: Report[]): ReportMetrics {
  const totalReports = reports.length
  const totalSurfaceM2 = reports.reduce((sum, r) => sum + (r.surfaceM2 || 0), 0)
  const totalBudgetEstimated = reports.reduce((sum, r) => sum + (r.budgetEstimated || 0), 0)

  const doneCount = reports.filter((r) => r.status === 'termine').length
  const progressPercent = totalReports === 0 ? 0 : Math.round((doneCount / totalReports) * 100)

  return {
    totalReports,
    totalSurfaceM2,
    totalBudgetEstimated,
    progressPercent
  }
}
