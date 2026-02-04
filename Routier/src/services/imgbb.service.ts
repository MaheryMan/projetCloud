/**
 * Service pour uploader des images vers ImgBB
 * ImgBB API: https://imgbb.com/api
 */

const IMGBB_API_KEY = import.meta.env.VITE_IMGBB_API_KEY
const IMGBB_API_URL = 'https://api.imgbb.com/1/upload'

interface ImgBBResponse {
  data: {
    url: string
    display_url: string
    delete_url: string
  }
  success: boolean
  status: number
}

/**
 * Compresse une image pour réduire la taille avant upload
 * @param file - Fichier image
 * @param maxWidth - Largeur maximale (default 1024)
 * @param maxHeight - Hauteur maximale (default 1024)
 * @param quality - Qualité JPEG (0-1, default 0.8)
 * @returns Promise<Blob> - Image compressée
 */
export async function compressImage(
  file: File,
  maxWidth: number = 1024,
  maxHeight: number = 1024,
  quality: number = 0.8
): Promise<Blob> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()

    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        const canvas = document.createElement('canvas')
        let width = img.width
        let height = img.height

        // Calculer les dimensions réduites
        if (width > height) {
          if (width > maxWidth) {
            height = Math.round((height * maxWidth) / width)
            width = maxWidth
          }
        } else {
          if (height > maxHeight) {
            width = Math.round((width * maxHeight) / height)
            height = maxHeight
          }
        }

        canvas.width = width
        canvas.height = height

        const ctx = canvas.getContext('2d')
        if (!ctx) {
          reject(new Error('Impossible de créer le contexte canvas'))
          return
        }

        ctx.drawImage(img, 0, 0, width, height)

        canvas.toBlob(
          (blob) => {
            if (blob) {
              resolve(blob)
            } else {
              reject(new Error('Erreur lors de la compression'))
            }
          },
          'image/jpeg',
          quality
        )
      }

      img.onerror = () => reject(new Error('Erreur lors du chargement de l\'image'))
      img.src = e.target?.result as string
    }

    reader.onerror = () => reject(new Error('Erreur lors de la lecture du fichier'))
    reader.readAsDataURL(file)
  })
}

/**
 * Upload une image vers ImgBB
 * @param file - Fichier image à uploader
 * @returns Promise<string> - URL de l'image uploadée
 */
export async function uploadImageToImgBB(file: File): Promise<string> {
  if (!IMGBB_API_KEY) {
    throw new Error('VITE_IMGBB_API_KEY manquante dans .env')
  }

  // Compresser l'image avant upload
  const compressedBlob = await compressImage(file, 1024, 1024, 0.85)

  // Créer FormData
  const formData = new FormData()
  formData.append('image', compressedBlob, file.name)
  formData.append('expiration', '31536000') // 1 an en secondes (optionnel)

  try {
    const response = await fetch(`${IMGBB_API_URL}?key=${IMGBB_API_KEY}`, {
      method: 'POST',
      body: formData
    })

    if (!response.ok) {
      throw new Error(`Erreur ImgBB: ${response.statusText}`)
    }

    const data: ImgBBResponse = await response.json()

    if (!data.success) {
      throw new Error('Erreur lors de l\'upload ImgBB')
    }

    return data.data.display_url
  } catch (error) {
    console.error('Erreur upload ImgBB:', error)
    throw new Error(`Impossible d'uploader l'image: ${error instanceof Error ? error.message : 'Erreur inconnue'}`)
  }
}

/**
 * Upload plusieurs images en parallèle (max 5)
 * @param files - Fichiers à uploader
 * @param maxPhotos - Nombre maximum de photos (default 5)
 * @returns Promise<string[]> - URLs des images uploadées
 */
export async function uploadImagesToImgBB(
  files: File[],
  maxPhotos: number = 5
): Promise<string[]> {
  if (files.length > maxPhotos) {
    throw new Error(`Maximum ${maxPhotos} photos autorisées`)
  }

  try {
    const uploadPromises = files.map((file) => uploadImageToImgBB(file))
    const urls = await Promise.all(uploadPromises)
    return urls
  } catch (error) {
    console.error('Erreur upload multiple:', error)
    throw error
  }
}
