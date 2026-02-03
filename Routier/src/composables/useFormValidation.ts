import { computed, Ref } from 'vue'

export function useFormValidation(fields: Record<string, Ref<string>>) {
  const errors = computed(() => {
    const result: Record<string, string> = {}

    Object.entries(fields).forEach(([key, refValue]) => {
      if (!refValue.value || !refValue.value.toString().trim()) {
        result[key] = 'Ce champ est obligatoire'
      }
    })

    return result
  })

  const hasErrors = computed(() => Object.keys(errors.value).length > 0)

  return {
    errors,
    hasErrors
  }
}
