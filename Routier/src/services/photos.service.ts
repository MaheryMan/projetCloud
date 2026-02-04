/**
 * Service pour gérer la collection "photos" dans Firebase
 * Chaque photo est liée à un report et un utilisateur
 */

import { db } from './firebase'
import {
  collection,
  addDoc,
  deleteDoc,
  doc,
  query,
  where,
  getDocs,
  Timestamp as FsTimestamp
} from 'firebase/firestore'
import type { Photo } from '@/types/report.types'

const PHOTOS_COLLECTION = 'photos'

/**
 * Normalise un document photo depuis Firestore
 */
function normalizePhoto(id: string, data: any): Photo {
  const uploadedAt = data.uploadedAt instanceof FsTimestamp
    ? data.uploadedAt.toDate()
    : data.uploadedAt

  return {
    id,
    reportId: data.reportId,
    uid: data.uid,
    imgbbUrl: data.imgbbUrl,
    uploadedAt
  }
}

/**
 * Ajoute une photo à Firebase
 * @param photo - Données de la photo (sans id)
 * @returns Promise<Photo> - Photo créée avec ID
 */
export async function addPhoto(
  photo: Omit<Photo, 'id' | 'uploadedAt'>
): Promise<Photo> {
  const ref = await addDoc(collection(db, PHOTOS_COLLECTION), {
    ...photo,
    uploadedAt: new Date()
  })

  return {
    id: ref.id,
    ...photo,
    uploadedAt: new Date()
  }
}

/**
 * Ajoute plusieurs photos (batch)
 * @param photos - Tableau de photos sans ID
 * @returns Promise<Photo[]> - Photos créées avec IDs
 */
export async function addPhotos(
  photos: Omit<Photo, 'id' | 'uploadedAt'>[]
): Promise<Photo[]> {
  const createdPhotos = await Promise.all(
    photos.map((photo) => addPhoto(photo))
  )
  return createdPhotos
}

/**
 * Supprime une photo
 * @param photoId - ID de la photo
 */
export async function deletePhoto(photoId: string): Promise<void> {
  await deleteDoc(doc(db, PHOTOS_COLLECTION, photoId))
}

/**
 * Supprime toutes les photos d'un signalement
 * @param reportId - ID du signalement
 */
export async function deletePhotosForReport(reportId: string): Promise<void> {
  const q = query(
    collection(db, PHOTOS_COLLECTION),
    where('reportId', '==', reportId)
  )
  const snap = await getDocs(q)

  const deletePromises = snap.docs.map((docSnap) =>
    deleteDoc(doc(db, PHOTOS_COLLECTION, docSnap.id))
  )

  await Promise.all(deletePromises)
}

/**
 * Récupère les photos d'un signalement
 * @param reportId - ID du signalement
 * @returns Promise<Photo[]> - Tableau des photos
 */
export async function getPhotosForReport(reportId: string): Promise<Photo[]> {
  const q = query(
    collection(db, PHOTOS_COLLECTION),
    where('reportId', '==', reportId)
  )
  const snap = await getDocs(q)

  return snap.docs
    .map((docSnap) => normalizePhoto(docSnap.id, docSnap.data()))
    .sort((a, b) => {
      const timeA = a.uploadedAt instanceof Date ? a.uploadedAt.getTime() : 0
      const timeB = b.uploadedAt instanceof Date ? b.uploadedAt.getTime() : 0
      return timeA - timeB
    })
}

/**
 * Récupère les photos d'un utilisateur
 * @param uid - ID utilisateur
 * @returns Promise<Photo[]> - Tableau des photos
 */
export async function getPhotosForUser(uid: string): Promise<Photo[]> {
  const q = query(
    collection(db, PHOTOS_COLLECTION),
    where('uid', '==', uid)
  )
  const snap = await getDocs(q)

  return snap.docs
    .map((docSnap) => normalizePhoto(docSnap.id, docSnap.data()))
    .sort((a, b) => {
      const timeA = a.uploadedAt instanceof Date ? a.uploadedAt.getTime() : 0
      const timeB = b.uploadedAt instanceof Date ? b.uploadedAt.getTime() : 0
      return timeB - timeA // Récentes d'abord
    })
}
