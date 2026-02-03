export type Id = string

export type Timestamp = Date | string

export type Nullable<T> = T | null

export interface ApiResponse<T> {
  data: T
  error?: string
}
