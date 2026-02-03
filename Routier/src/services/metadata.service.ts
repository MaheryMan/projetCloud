import { db } from './firebase'
import { collection, getDocs, query} from 'firebase/firestore'

const METADATA_COLLECTION = 'metadata'

export interface TypeSignalement {
  id: string | number
  code?: string
  libelle: string
  description?: string
  icone?: string
  couleur?: string
  niveauUrgence?: number
}

export interface Status {
  id: string | number
  code?: string
  libelle: string
}

/**
 * Récupère tous les types de signalement depuis la collection metadata
 * Les documents ont les IDs: type_signalement_1, type_signalement_2, etc.
 */
export async function fetchTypesSignalement(): Promise<TypeSignalement[]> {
  try {
    const q = query(collection(db, METADATA_COLLECTION))
    const snap = await getDocs(q)
    
    const types: TypeSignalement[] = []
    snap.docs.forEach((doc) => {
      const docId = doc.id
      // Récupérer les documents qui commencent par "type_signalement_"
      if (docId.startsWith('type_signalement_')) {
        const data = doc.data()
        types.push({
          id: data.id,
          code: String(data.id),
          libelle: data.libelle,
          description: data.description,
          icone: data.icone,
          couleur: data.couleur,
          niveauUrgence: data.niveauUrgence
        } as TypeSignalement)
      }
    })

    return types
  } catch (error) {
    console.error('Erreur lors de la récupération des types de signalement:', error)
    throw new Error('Impossible de récupérer les types de signalement')
  }
}

/**
 * Récupère tous les statuts depuis la collection metadata
 * Les documents ont les IDs: status_1, status_2, etc.
 */
export async function fetchStatuses(): Promise<Status[]> {
  try {
    const q = query(collection(db, METADATA_COLLECTION))
    const snap = await getDocs(q)
    
    const statuses: Status[] = []
    snap.docs.forEach((doc) => {
      const docId = doc.id
      // Récupérer les documents qui commencent par "status_"
      if (docId.startsWith('status_')) {
        const data = doc.data()
        statuses.push({
          id: data.id,
          code: String(data.id),
          libelle: data.libelle
        } as Status)
      }
    })

    return statuses
  } catch (error) {
    console.error('Erreur lors de la récupération des statuts:', error)
    throw new Error('Impossible de récupérer les statuts')
  }
}

/**
 * Récupère à la fois les types de signalement et les statuts
 */
export async function fetchAllMetadata() {
  try {
    const [types, statuses] = await Promise.all([
      fetchTypesSignalement(),
      fetchStatuses()
    ])

    return {
      types,
      statuses
    }
  } catch (error) {
    console.error('Erreur lors de la récupération des métadonnées:', error)
    throw new Error('Impossible de récupérer les métadonnées')
  }
}
