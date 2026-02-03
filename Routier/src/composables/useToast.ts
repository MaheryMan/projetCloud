import { toastController } from '@ionic/vue'

export function useToast() {
  const showToast = async (
    message: string,
    color: 'primary' | 'success' | 'warning' | 'danger' | 'medium' = 'primary',
    duration = 2500
  ) => {
    const toast = await toastController.create({
      message,
      color,
      duration,
      position: 'top'
    })
    await toast.present()
  }

  return { showToast }
}
