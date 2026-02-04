<template>
  <div class="photo-gallery">
    <!-- Loader -->
    <div v-if="loading" class="gallery-loader">
      <ion-spinner name="crescent"></ion-spinner>
      <p>Chargement des photos...</p>
    </div>

    <!-- Gallery vide -->
    <div v-else-if="photos.length === 0" class="gallery-empty">
      <ion-icon name="image-outline"></ion-icon>
      <p>Aucune photo pour ce signalement</p>
    </div>

    <!-- Gallery avec photos -->
    <div v-else class="gallery-container">
      <!-- Photo principale -->
      <div class="main-photo-wrapper">
        <img
          :src="photos[currentPhotoIndex].imgbbUrl"
          :alt="`Photo ${currentPhotoIndex + 1}`"
          class="main-photo"
          @error="onPhotoError"
        />
        <div class="photo-counter">
          <span>{{ currentPhotoIndex + 1 }}/{{ photos.length }}</span>
        </div>
      </div>

      <!-- Contrôles navigation -->
      <div v-if="photos.length > 1" class="gallery-controls">
        <button
          type="button"
          @click="previousPhoto"
          class="control-btn prev-btn"
          :disabled="currentPhotoIndex === 0"
          title="Photo précédente"
        >
          <span class="arrow-text">&lt;</span>
        </button>

        <div class="thumbnails">
          <button
            v-for="(photo, index) in photos"
            :key="index"
            type="button"
            class="thumbnail"
            :class="{ 'active': index === currentPhotoIndex }"
            @click="currentPhotoIndex = index"
          >
            <img
              :src="photo.imgbbUrl"
              :alt="`Miniature ${index + 1}`"
              class="thumbnail-image"
              @error="onPhotoError"
            />
          </button>
        </div>

        <button
          type="button"
          @click="nextPhoto"
          class="control-btn next-btn"
          :disabled="currentPhotoIndex === photos.length - 1"
          title="Photo suivante"
        >
          <span class="arrow-text">&gt;</span>
        </button>
      </div>

      <!-- Info photo -->
      <div v-if="photos[currentPhotoIndex]" class="photo-info">
        <div class="info-item">
          <span class="info-label">Uploadée le:</span>
          <span class="info-value">
            {{ formatDate(photos[currentPhotoIndex].uploadedAt) }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { IonSpinner } from '@ionic/vue'
import { getPhotosForReport } from '@/services/photos.service'
import type { Photo } from '@/types/report.types'

interface Props {
  reportId: string
}

const props = defineProps<Props>()

const photos = ref<Photo[]>([])
const loading = ref(false)
const currentPhotoIndex = ref(0)

onMounted(async () => {
  await loadPhotos()
})

const loadPhotos = async () => {
  loading.value = true
  try {
    photos.value = await getPhotosForReport(props.reportId)
    currentPhotoIndex.value = 0
  } catch (error) {
    console.error('Erreur chargement photos:', error)
  } finally {
    loading.value = false
  }
}

const previousPhoto = () => {
  if (currentPhotoIndex.value > 0) {
    currentPhotoIndex.value--
  }
}

const nextPhoto = () => {
  if (currentPhotoIndex.value < photos.value.length - 1) {
    currentPhotoIndex.value++
  }
}

const onPhotoError = () => {
  console.error('Erreur chargement image ImgBB')
}

const formatDate = (date: any): string => {
  if (!date) return 'Date inconnue'
  const dateObj = date instanceof Date ? date : new Date(date)
  return dateObj.toLocaleDateString('fr-FR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped lang="css">
.photo-gallery {
  width: 100%;
  padding: 0;
}

.gallery-loader,
.gallery-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  color: #999;
  min-height: 250px;
}

.gallery-loader ion-spinner {
  --color: #3b82f6;
  margin-bottom: 12px;
}

.gallery-empty ion-icon {
  font-size: 48px;
  color: #ddd;
  margin-bottom: 12px;
}

.gallery-empty p,
.gallery-loader p {
  margin: 0;
  font-size: 14px;
  color: #999;
}

.gallery-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.main-photo-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  background: #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.main-photo {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.photo-counter {
  position: absolute;
  bottom: 12px;
  right: 12px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.gallery-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 4px;
}

.control-btn {
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #374151;
  flex-shrink: 0;
}

.control-btn:hover:not(:disabled) {
  background: #e5e7eb;
  border-color: #d1d5db;
}

.control-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.control-btn ion-icon {
  font-size: 20px;
}

.arrow-text {
  font-size: 24px;
  font-weight: bold;
  line-height: 1;
}

.thumbnails {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  flex: 1;
  padding: 0 4px;
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch;
}

.thumbnails::-webkit-scrollbar {
  height: 4px;
}

.thumbnails::-webkit-scrollbar-track {
  background: #f0f0f0;
  border-radius: 2px;
}

.thumbnails::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 2px;
}

.thumbnails::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}

.thumbnail {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  border: 2px solid transparent;
  padding: 0;
  background: none;
  cursor: pointer;
  overflow: hidden;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.thumbnail:hover {
  border-color: #3b82f6;
  transform: scale(1.05);
}

.thumbnail.active {
  border-color: #3b82f6;
  box-shadow: 0 0 0 1px #3b82f6;
}

.thumbnail-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.photo-info {
  padding: 12px;
  background: #f9fafb;
  border-radius: 6px;
  display: flex;
  gap: 16px;
  font-size: 13px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  color: #6b7280;
  font-weight: 500;
}

.info-value {
  color: #374151;
  font-weight: 600;
}
</style>
