/**
 * Normalise une string pour les comparaisons case-insensitive
 * Convertit en minuscules et remplace les espaces par des underscores
 * @param str - La chaîne à normaliser
 * @returns La chaîne normalisée
 */
export function normalizeString(str: string): string {
  if (!str) return ''
  return str.toLowerCase().replace(/\s+/g, '_')
}

/**
 * Compare deux strings de manière case-insensitive et space-insensitive
 * @param str1 - Première chaîne
 * @param str2 - Deuxième chaîne
 * @returns true si les chaînes sont égales après normalisation
 */
export function compareNormalized(str1: string, str2: string): boolean {
  return normalizeString(str1) === normalizeString(str2)
}

/**
 * Trouve un élément dans un tableau par comparaison normalisée
 * @param array - Le tableau à chercher
 * @param value - La valeur à chercher (sera normalisée)
 * @param accessor - Fonction pour accéder à la propriété à comparer
 * @returns L'élément trouvé ou undefined
 */
export function findNormalized<T>(
  array: T[],
  value: string,
  accessor: (item: T) => string
): T | undefined {
  const normalizedValue = normalizeString(value)
  return array.find((item) => normalizeString(accessor(item)) === normalizedValue)
}
