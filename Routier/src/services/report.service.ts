import { db } from './firebase'
import { collection, addDoc, getDocs, query, where, Timestamp as FsTimestamp } from 'firebase/firestore'
import type { Report } from '@/types/report.types'

const REPORTS_COLLECTION = 'reports'

function normalizeReport(id: string, data: any): Report {
  const createdAt = data.createdAt instanceof FsTimestamp ? data.createdAt.toDate() : data.createdAt

  const rawStatus = data.status
  const status =
    rawStatus === 'pending'
      ? 'nouveau'
      : rawStatus === 'approved'
        ? 'termine'
        : rawStatus === 'rejected'
          ? 'nouveau'
          : rawStatus

  return {
    id,
    uid: data.uid,
    description: data.description,
    type: data.type,
    lat: data.lat,
    lng: data.lng,
    status,
    surfaceM2: data.surfaceM2 ? Number(data.surfaceM2) : undefined,
    budgetEstimated: data.budgetEstimated ? Number(data.budgetEstimated) : undefined,
    companyName: data.companyName || undefined,
    photo: data.photo || undefined,
    createdAt
  }
}

export async function createReport(payload: Omit<Report, 'id' | 'createdAt'>): Promise<Report> {
  const ref = await addDoc(collection(db, REPORTS_COLLECTION), {
    ...payload,
    createdAt: new Date()
  })

  return {
    id: ref.id,
    ...payload,
    createdAt: new Date()
  }
}

export async function getAllReports(): Promise<Report[]> {
  const snap = await getDocs(collection(db, REPORTS_COLLECTION))

  return snap.docs.map((docSnap) => normalizeReport(docSnap.id, docSnap.data()))
}

export async function getReportsByUser(uid: string): Promise<Report[]> {
  const q = query(collection(db, REPORTS_COLLECTION), where('uid', '==', uid))
  const snap = await getDocs(q)

  return snap.docs.map((docSnap) => normalizeReport(docSnap.id, docSnap.data()))
}
