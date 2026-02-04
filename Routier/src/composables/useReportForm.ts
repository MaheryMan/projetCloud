import { ref, computed, type Ref } from 'vue'
import { Camera, CameraResultType, CameraSource } from '@capacitor/camera'
import { createReport } from '@/services/report.service'
import { addPhotos } from '@/services/photos.service'
import { useMetadataStore } from '@/stores/metadata.store'
import type { ReportType } from '@/types/report.types'
import type { TypeSignalement } from '@/services/metadata.service'

type ToastColor = 'primary' | 'success' | 'warning' | 'danger' | 'medium'
type ShowToast = (message: string, color?: ToastColor, duration?: number) => Promise<void> | void

const FIRESTORE_MAX_FIELD_BYTES = 1048487
const THUMB_TARGET_MAX_BYTES = 350_000
const THUMB_MAX_DIMENSION = 1024

type RouterLike = {
  push: (to: string) => any
}

export interface UseReportFormOptions {
  isAuthenticated: Ref<boolean>
  userId: Ref<string | null>
  newPosition: Ref<{ lat: number; lng: number }>
  showToast: ShowToast
  router: RouterLike
  onCreated?: () => void
}

export function useReportForm(options: UseReportFormOptions) {
  const metadataStore = useMetadataStore()
  
  const showForm = ref(false)
  const submitting = ref(false)

  /**
   * Récupère le libellé du statut initial depuis la metadata
   * Utilise 'cree' pour les brouillons (non affichés jusqu'à validation)
   */
  const getInitialStatusLabel = (): string => {
    const creeStatus = metadataStore.statuses.find((s) => s.code === 'cree')
    return creeStatus?.libelle || 'Créé'
  }

  // Ces variables doivent rester modifiables directement
  const reportType = ref<string>('Trou')
  const reportDescription = ref('')
  const surfaceM2 = ref<number>(0)
  
  // Ref pour le composant PhotoPicker
  const photoPickerRef = ref<any>(null)

  const showActionSheet = ref(false)

  /**
   * Retourne les types de signalement depuis le store Firebase
   */
  const availableTypes = computed(() => {
    const types = metadataStore.types
    
    // Mapping entre les icônes du backend et les icônes Ionicons
    const iconMapping: Record<string, string> = {
      'pothole': 'warning-outline',
      'diversion': 'swap-horizontal-outline',
      'construction': 'construct-outline',
      'other': 'help-outline',
      'alert': 'alert-circle-outline',
      'danger': 'warning-outline',
      'info': 'information-circle-outline'
    }
    
    // Si les types sont vides, retourner des valeurs par défaut
    if (!types || types.length === 0) {
      return [
        { value: 'Trou', label: 'Trou', icon: 'warning-outline', class: 'danger' },
        { value: 'Chantier', label: 'Chantier', icon: 'construct-outline', class: 'warning' },
        { value: 'Déviation', label: 'Déviation', icon: 'swap-horizontal-outline', class: 'info' },
        { value: 'Autre', label: 'Autre', icon: 'help-outline', class: 'default' }
      ]
    }
    
    const mapped = types.map((type) => {
      // Utiliser le mapping pour l'icône, sinon utiliser l'icône du backend ou une icône par défaut
      const iconeBackend = (type.icone || 'alert').toLowerCase()
      const ioniconsIcon = iconMapping[iconeBackend] || 'help-outline'
      
      const mapped = {
        value: type.libelle,
        label: type.libelle,
        icon: ioniconsIcon,
        class: type.couleur ? '' : 'default',
        color: type.couleur
      }
      return mapped
    })
    
    return mapped
  })

  const estimateDataUrlBytes = (dataUrl: string): number => {
    if (!dataUrl) return 0
    const commaIndex = dataUrl.indexOf(',')
    const base64 = commaIndex >= 0 ? dataUrl.slice(commaIndex + 1) : dataUrl
    const len = base64.length

    // base64 length -> bytes approximation
    let padding = 0
    if (base64.endsWith('==')) padding = 2
    else if (base64.endsWith('=')) padding = 1

    return Math.floor((len * 3) / 4) - padding
  }

  const validatePhotoSize = (dataUrl: string): boolean => {
    if (!dataUrl) return true
    const bytes = estimateDataUrlBytes(dataUrl)
    if (bytes <= THUMB_TARGET_MAX_BYTES) return true

    options.showToast(
      `Vignette trop volumineuse (${Math.round(bytes / 1024)} Ko). Limite ~${Math.round(
        THUMB_TARGET_MAX_BYTES / 1024
      )} Ko.`,
      'warning'
    )

    return false
  }

  const dataUrlToImage = (dataUrl: string): Promise<HTMLImageElement> => {
    return new Promise((resolve, reject) => {
      const img = new Image()
      img.onload = () => resolve(img)
      img.onerror = reject
      img.src = dataUrl
    })
  }

  const blobToDataUrl = (blob: Blob): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onloadend = () => resolve(String(reader.result || ''))
      reader.onerror = reject
      reader.readAsDataURL(blob)
    })
  }

  const canvasToBlob = (canvas: HTMLCanvasElement, type: string, quality: number): Promise<Blob> => {
    return new Promise((resolve, reject) => {
      canvas.toBlob(
        (blob) => {
          if (!blob) reject(new Error('canvas_to_blob_failed'))
          else resolve(blob)
        },
        type,
        quality
      )
    })
  }

  const createThumbnail = async (inputDataUrl: string): Promise<string> => {
    const img = await dataUrlToImage(inputDataUrl)

    let maxDim = THUMB_MAX_DIMENSION
    let quality = 0.75

    const tryEncode = async (mime: string): Promise<string> => {
      const scale = Math.min(1, maxDim / Math.max(img.width, img.height))
      const width = Math.max(1, Math.round(img.width * scale))
      const height = Math.max(1, Math.round(img.height * scale))

      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height

      const ctx = canvas.getContext('2d')
      if (!ctx) throw new Error('canvas_context_failed')

      ctx.drawImage(img, 0, 0, width, height)

      const blob = await canvasToBlob(canvas, mime, quality)
      return blobToDataUrl(blob)
    }

    for (let attempt = 0; attempt < 8; attempt++) {
      let out = ''
      try {
        out = await tryEncode('image/webp')
      } catch {
        out = await tryEncode('image/jpeg')
      }

      if (estimateDataUrlBytes(out) <= THUMB_TARGET_MAX_BYTES) {
        return out
      }

      quality = Math.max(0.35, quality - 0.1)
      maxDim = Math.max(480, Math.round(maxDim * 0.85))
    }

    throw new Error('photo_thumbnail_too_large')
  }

  const validateForm = (): boolean => {
    if (!reportType.value) {
      options.showToast('Veuillez sélectionner un type de signalement', 'warning')
      return false
    }

    if (reportDescription.value.trim().length < 5) {
      options.showToast('Veuillez saisir une description (au moins 5 caractères)', 'warning')
      return false
    }

    if (Number.isNaN(Number(surfaceM2.value)) || Number(surfaceM2.value) < 0) {
      options.showToast('Surface invalide (doit être >= 0)', 'warning')
      return false
    }

    return true
  }

  const formProgress = computed(() => {
    let progress = 0
    if (reportType.value) progress += 50
    if (reportDescription.value.trim().length >= 5) progress += 50
    return progress
  })

  const canSubmit = computed(() => {
    return reportType.value && reportDescription.value.trim().length >= 5
  })

  const placeholderText = computed(() => {
    const placeholders: Record<string, string> = {
      trou: 'Ex: Grand trou sur la route principale causant des dégâts aux véhicules...',
      chantier: 'Ex: Travaux de réfection en cours, circulation alternée...',
      deviation: 'Ex: Route fermée, déviation par la RN2...'
    }
    return placeholders[reportType.value] || 'Décrivez le problème...'
  })

  const closeForm = () => {
    showForm.value = false
    photoPickerRef.value?.reset()
  }

  const openPhotoOptions = () => {
    // Le PhotoPicker gère ses propres options internes
    photoPickerRef.value?.openPhotoOptions()
  }

  const requireAuthOrRedirect = (): boolean => {
    if (options.isAuthenticated.value && options.userId.value) return true
    options.showToast('Veuillez vous connecter pour signaler un problème', 'warning')
    options.router.push('/login')
    return false
  }

  const resetFormState = () => {
    reportType.value = 'trou'
    reportDescription.value = ''
    surfaceM2.value = 0
    photoPickerRef.value?.reset()
  }

  const startAddReport = () => {
    if (document.hidden) return
    if (!requireAuthOrRedirect()) return

    resetFormState()
    showForm.value = true
  }

  const startAddReportAtPosition = (position: { lat: number; lng: number }) => {
    if (!requireAuthOrRedirect()) return

    options.newPosition.value = { lat: position.lat, lng: position.lng }
    resetFormState()
    showForm.value = true
  }

  const submitReport = async () => {
    if (document.hidden) return
    if (!requireAuthOrRedirect()) return
    if (!canSubmit.value) {
      options.showToast('Formulaire invalide', 'warning')
      return
    }

    if (!validateForm()) return

    submitting.value = true

    try {
      // Créer le report d'abord
      const reportData: any = {
        uid: options.userId.value as string,
        description: reportDescription.value,
        type: reportType.value as any,
        lat: options.newPosition.value.lat,
        lng: options.newPosition.value.lng,
        status: getInitialStatusLabel(),
        surfaceM2: Number(surfaceM2.value || 0)
      }

      const createdReport = await createReport(reportData)

      // Uploader les photos ImgBB et créer les documents photos Firebase
      if (photoPickerRef.value && photoPickerRef.value.getPhotoCount() > 0) {
        try {
          const photoUrls = await photoPickerRef.value.uploadPhotos()
          
          // Créer les documents photos dans Firebase
          if (photoUrls && photoUrls.length > 0) {
            const photoDocuments = photoUrls.map((url: string) => ({
              reportId: createdReport.id,
              uid: options.userId.value,
              imgbbUrl: url
            }))
            
            await addPhotos(photoDocuments)
          }
        } catch (photoError) {
          console.error('Erreur lors de l\'upload des photos:', photoError)
          options.showToast('Photos non uploadées mais le signalement a été créé', 'warning')
          // Continuer malgré l'erreur des photos
        }
      }

      options.showToast('Signalement créé avec succès!')
      closeForm()
      options.onCreated?.()
    } catch (error) {
      options.showToast('Erreur lors de la création du signalement', 'danger')
      console.error('Erreur lors de la création:', error)
    } finally {
      submitting.value = false
    }
  }

  return {
    showForm,
    submitting,
    reportType,
    reportDescription,
    surfaceM2,
    photoPickerRef,
    formProgress,
    canSubmit,
    placeholderText,
    availableTypes,
    startAddReport,
    startAddReportAtPosition,
    closeForm,
    openPhotoOptions,
    submitReport
  }
}
