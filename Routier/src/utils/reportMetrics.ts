import type { Report } from '@/types/report.types'

export interface ReportMetrics {
  totalReports: number
  totalSurfaceM2: number
  totalBudgetEstimated: number
  progressPercent: number
}

/**
 * Retourne le pourcentage d'avancement selon le statut
 * Utilise la même formule que le backend:
 * Nouveau = 0%, En cours = 50%, Terminé = 100%
 * Avancement = ((En cours × 0.5) + Terminé) / Total × 100
 */
export function getStatusProgress(status: string): number {
  if (!status) return 0
  
  const normalizedStatus = status.toLowerCase().trim()
  
  // Terminé = 100% (statut code: REPORT003)
  if (
    normalizedStatus.includes('termin') || 
    normalizedStatus === 'termine' ||
    normalizedStatus === 'approved' ||
    normalizedStatus === 'terminé'
  ) {
    return 100
  }
  
  // En cours = 50% (statut code: REPORT002)
  if (
    normalizedStatus.includes('encours') || 
    normalizedStatus === 'en_cours' ||
    normalizedStatus === 'en cours' ||
    normalizedStatus === 'in_progress' ||
    normalizedStatus === 'inprogress'
  ) {
    return 50
  }
  
  // Nouveau = 0% (statut code: REPORT001) ou autre
  // Inclut aussi: "nouveau", "pending", "en attente", "créé", etc.
  return 0
}

export function computeReportMetrics(reports: Report[]): ReportMetrics {
  const totalReports = reports.length
  const totalSurfaceM2 = reports.reduce((sum, r) => sum + (r.surfaceM2 || 0), 0)
  const totalBudgetEstimated = reports.reduce((sum, r) => sum + (r.budgetEstimated || 0), 0)

  // Calcul de l'avancement global basé sur le statut de chaque signalement
  // Formule identique au backend: ((En cours × 0.5) + Terminé) / Total × 100
  // Où: Nouveau = 0%, En cours = 50%, Terminé = 100%
  // 
  // Exemple: 2 Nouveaux, 1 En cours, 1 Terminé
  // = (0 + 0 + 50 + 100) / 4 = 150 / 4 = 37.5% ≈ 38%
  // Ou: ((1 × 0.5) + 1) / 4 × 100 = 1.5 / 4 × 100 = 37.5% ≈ 38%
  
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

  // DEBUG: Log le calcul exact pour comparaison avec le backend
  const newCount = reports.filter(r => getStatusProgress(r.status) === 0).length
  const inProgressCount = reports.filter(r => getStatusProgress(r.status) === 50).length
  const finishedCount = reports.filter(r => getStatusProgress(r.status) === 100).length
  
  console.debug(
    `[ReportMetrics] Avancement: 
    Nouveau(0%)=${newCount}, En cours(50%)=${inProgressCount}, Terminé(100%)=${finishedCount}
    Calcul: ((${inProgressCount} × 0.5) + ${finishedCount}) / ${totalReports} × 100 = ${progressPercent}%`
  )

  return {
    totalReports,
    totalSurfaceM2,
    totalBudgetEstimated,
    progressPercent
  }
}
