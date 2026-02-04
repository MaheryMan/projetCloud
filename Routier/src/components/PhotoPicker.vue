<template>
  <div class="photo-picker">
    <!-- Photos existantes -->
    <div class="photos-container">
      <transition-group name="photo-fade" tag="div" class="photos-grid">
        <div
          v-for="(photo, index) in selectedPhotos"
          :key="index"
          class="photo-item"
        >
          <img :src="photo.preview" :alt="`Photo ${index + 1}`" class="photo-image" />
          <div class="photo-overlay">
            <button
              type="button"
              @click="removePhoto(index)"
              class="remove-btn"
              :disabled="isUploading"
            >
              <ion-icon name="trash-outline"></ion-icon>
            </button>
          </div>
          <span class="photo-counter">{{ index + 1 }}/{{ selectedPhotos.length }}</span>
        </div>
      </transition-group>
    </div>

    <!-- Zone pour ajouter photos -->
    <div v-if="selectedPhotos.length < MAX_PHOTOS" class="add-photo-section">
      <button
        type="button"
        @click="openPhotoOptions"
        class="add-photo-btn"
        :disabled="isUploading"
      >
        <ion-icon name="camera-outline" class="add-icon"></ion-icon>
        <span class="add-text">
          Ajouter une photo ({{ selectedPhotos.length }}/{{ MAX_PHOTOS }})
        </span>
      </button>
    </div>

    <!-- Message limite atteinte -->
    <div v-if="selectedPhotos.length >= MAX_PHOTOS" class="limit-message">
      <ion-icon name="information-circle-outline"></ion-icon>
      <span>Limite de {{ MAX_PHOTOS }} photos atteinte</span>
    </div>

    <!-- Loader upload -->
    <transition name="fade">
      <div v-if="isUploading" class="upload-progress">
        <div class="progress-item">
          <ion-spinner name="crescent"></ion-spinner>
          <span class="progress-text">{{ uploadingText }}</span>
        </div>
      </div>
    </transition>

    <!-- Caméra/Galerie -->
    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      capture="environment"
      @change="handlePhotoSelected"
      hidden
    />

    <!-- Action Sheet -->
    <ion-action-sheet
      :is-open="showActionSheet"
      header="Ajouter une photo"
      :buttons="actionButtons"
      @didDismiss="showActionSheet = false"
    ></ion-action-sheet>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  IonIcon,
  IonSpinner,
  IonActionSheet
} from '@ionic/vue'
import { Camera, CameraResultType, CameraSource } from '@capacitor/camera'
import { uploadImageToImgBB } from '@/services/imgbb.service'

const MAX_PHOTOS = 5
const MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB

interface PhotoItem {
  preview: string // Data URL pour preview
  file?: File
}

// Props & Emits
defineProps<{
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:photos': [urls: string[]]
  'error': [message: string]
}>()

// Refs
const selectedPhotos = ref<PhotoItem[]>([])
const fileInput = ref<HTMLInputElement | null>(null)
const showActionSheet = ref(false)
const isUploading = ref(false)
const uploadingText = ref('Compression...')

// Computed
const actionButtons = computed(() => [
  {
    text: 'Caméra',
    icon: 'camera-outline',
    handler: async () => {
      try {
        const image = await Camera.getPhoto({
          quality: 90,
          allowEditing: false,
          resultType: CameraResultType.Uri,
          source: CameraSource.Camera
        })

        if (image.webPath) {
          await addPhotoFromWebPath(image.webPath)
        }
      } catch (error) {
        if ((error as any)?.message !== 'User cancelled photos app') {
          emit('error', 'Erreur lors de la capture de la photo')
        }
      }
    }
  },
  {
    text: 'Galerie',
    icon: 'image-outline',
    handler: async () => {
      try {
        const image = await Camera.getPhoto({
          quality: 90,
          allowEditing: false,
          resultType: CameraResultType.Uri,
          source: CameraSource.Photos
        })

        if (image.webPath) {
          await addPhotoFromWebPath(image.webPath)
        }
      } catch (error) {
        if ((error as any)?.message !== 'User cancelled photos app') {
          emit('error', 'Erreur lors de la sélection de la photo')
        }
      }
    }
  },
  {
    text: 'Annuler',
    role: 'cancel'
  }
])

// Méthodes
const openPhotoOptions = () => {
  if (selectedPhotos.value.length >= MAX_PHOTOS) {
    emit('error', `Limite de ${MAX_PHOTOS} photos atteinte`)
    return
  }
  showActionSheet.value = true
}

const addPhotoFromWebPath = async (webPath: string) => {
  try {
    const response = await fetch(webPath)
    const blob = await response.blob()
    const file = new File([blob], 'photo.jpg', { type: 'image/jpeg' })

    await addPhotoFile(file)
  } catch (error) {
    emit('error', 'Erreur lors du traitement de la photo')
  }
}

const addPhotoFile = async (file: File) => {
  if (selectedPhotos.value.length >= MAX_PHOTOS) {
    emit('error', `Maximum ${MAX_PHOTOS} photos autorisées`)
    return
  }

  if (file.size > MAX_FILE_SIZE) {
    emit('error', `Fichier trop volumineux (max ${MAX_FILE_SIZE / 1024 / 1024}MB)`)
    return
  }

  // Vérifier le type MIME
  if (!file.type.startsWith('image/')) {
    emit('error', 'Veuillez sélectionner une image valide')
    return
  }

  // Créer preview
  const reader = new FileReader()
  reader.onload = (e) => {
    const preview = e.target?.result as string
    selectedPhotos.value.push({
      preview,
      file
    })
  }
  reader.readAsDataURL(file)
}

const removePhoto = (index: number) => {
  selectedPhotos.value.splice(index, 1)
  emitPhotos()
}

const handlePhotoSelected = async (event: Event) => {
  const input = event.target as HTMLInputElement
  if (input.files && input.files[0]) {
    await addPhotoFile(input.files[0])
  }
  // Reset input
  input.value = ''
}

const emitPhotos = () => {
  // Émettre uniquement les URLs (preview pour l'instant)
  const urls = selectedPhotos.value.map((p) => p.preview)
  emit('update:photos', urls)
}

/**
 * Upload les photos vers ImgBB
 * Doit être appelé depuis le parent quand le formulaire est soumis
 */
async function uploadPhotos(): Promise<string[]> {
  if (selectedPhotos.value.length === 0) {
    return []
  }

  const filesToUpload = selectedPhotos.value.filter((p) => p.file).map((p) => p.file!) as File[]

  if (filesToUpload.length === 0) {
    return selectedPhotos.value.map((p) => p.preview)
  }

  isUploading.value = true
  const uploadedUrls: string[] = []

  try {
    for (let i = 0; i < filesToUpload.length; i++) {
      uploadingText.value = `Upload ${i + 1}/${filesToUpload.length}...`
      const url = await uploadImageToImgBB(filesToUpload[i])
      uploadedUrls.push(url)
    }

    return uploadedUrls
  } catch (error) {
    emit('error', `Erreur upload: ${error instanceof Error ? error.message : 'Inconnue'}`)
    throw error
  } finally {
    isUploading.value = false
  }
}

/**
 * Réinitialise le picker
 */
function reset() {
  selectedPhotos.value = []
  if (fileInput.value) {
    fileInput.value.value = ''
  }
  emit('update:photos', [])
}

/**
 * Retourne le nombre de photos
 */
function getPhotoCount(): number {
  return selectedPhotos.value.length
}

// Exposer les méthodes au parent via ref template
defineExpose({
  uploadPhotos,
  reset,
  getPhotoCount,
  openPhotoOptions
})
</script>

<style scoped lang="css">
.photo-picker {
  width: 100%;
}

.photos-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 12px;
}

.photos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 10px;
  padding: 0 4px;
}

.photo-item {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  aspect-ratio: 1;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
}

.photo-item:hover {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.photo-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.photo-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.photo-item:hover .photo-overlay {
  opacity: 1;
}

.remove-btn {
  background: rgba(255, 255, 255, 0.9);
  border: none;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #ff4444;
}

.remove-btn:hover {
  background: #fff;
  transform: scale(1.1);
}

.remove-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.photo-counter {
  position: absolute;
  bottom: 4px;
  right: 4px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 3px;
}

.add-photo-section {
  margin-bottom: 12px;
}

.add-photo-btn {
  width: 100%;
  padding: 16px;
  border: 2px dashed #ccc;
  border-radius: 8px;
  background: #fafafa;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #666;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 500;
}

.add-photo-btn:hover {
  border-color: #999;
  background: #f5f5f5;
  color: #333;
}

.add-photo-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.add-icon {
  font-size: 28px;
  color: #999;
}

.add-text {
  font-size: 13px;
}

.limit-message {
  padding: 12px;
  background: #fff3cd;
  border: 1px solid #ffc107;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #856404;
  font-size: 13px;
}

.limit-message ion-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.upload-progress {
  padding: 16px;
  background: #e3f2fd;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  font-size: 13px;
  color: #1976d2;
}

.progress-item ion-spinner {
  --color: #1976d2;
}

.progress-text {
  flex: 1;
}

/* Animations */
.photo-fade-enter-active,
.photo-fade-leave-active {
  transition: all 0.3s ease;
}

.photo-fade-enter-from {
  opacity: 0;
  transform: scale(0.8);
}

.photo-fade-leave-to {
  opacity: 0;
  transform: scale(0.8);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
