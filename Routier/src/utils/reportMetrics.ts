import type { Report } from '@/types/report.types'

export interface ReportMetrics {
  totalReports: number
  totalSurfaceM2: number
  totalBudgetEstimated: number
  progressPercent: number
}

/**
 * Retourne le pourcentage d'avancement selon le statut
 * Nouveau = 0%, En cours = 50%, Terminé = 100%
 */
export function getStatusProgress(status: string): number {
  const normalizedStatus = status.toLowerCase().replace(/[\s_-]+/g, '')
  
  // Terminé = 100%
  if (normalizedStatus.includes('termin') || normalizedStatus === 'termine') {
    return 100
  }
  
  // En cours = 50%
  if (normalizedStatus.includes('encours') || normalizedStatus === 'en_cours') {
    return 50
  }
  
  // Nouveau ou autre = 0%
  return 0
}

export function computeReportMetrics(reports: Report[]): ReportMetrics {
  const totalReports = reports.length
  const totalSurfaceM2 = reports.reduce((sum, r) => sum + (r.surfaceM2 || 0), 0)
  const totalBudgetEstimated = reports.reduce((sum, r) => sum + (r.budgetEstimated || 0), 0)

  // Calcul de l'avancement global basé sur le statut de chaque signalement
  // Nouveau = 0%, En cours = 50%, Terminé = 100%
  if (totalReports === 0) {
    return {
      totalReports,
      totalSurfaceM2,
      totalBudgetEstimated,
      progressPercent: 0
    }
  }

  const totalProgress = reports.reduce((sum, r) => sum + getStatusProgress(r.status), 0)
  const progressPercent = Math.round(totalProgress / totalReports)

  return {
    totalReports,
    totalSurfaceM2,
    totalBudgetEstimated,
    progressPercent
  }
}
