// Shared types for the H5 roadbook app

export interface RouteSummary {
  id: number
  userId: number
  title: string
  description?: string
  totalDays: number
  totalDistance?: number
  startPoint?: string
  endPoint?: string
  startLng?: number
  startLat?: number
  endLng?: number
  endLat?: number
  status: number
  isPublic: number
  viewCount: number
  createdAt: string
  updatedAt?: string
  templateId?: number
  thumbnailUrl?: string
}

export interface WaypointDetail {
  sort: number
  type: string
  name: string
  description?: string
  tips?: string
  photoUrl?: string
  lng: number
  lat: number
  arrival?: string
  departure?: string
  stayMin?: number
  distanceFromPrevKm?: number
  driveScore?: number
  parkingScore?: number
  roadScore?: number
  isBreak?: boolean
}

export interface DayItinerary {
  day: number
  distanceKm: number
  driveTimeMin: number
  summary?: string
  waypoints: WaypointDetail[]
}

export interface EstimatedCost {
  tollYuan: number
  fuelYuan: number
  totalYuan: number
}

export interface FuelStop {
  day: number
  location: string
  lng: number
  lat: number
  reason: string
}

export interface RouteDetail {
  routeId: number
  title: string
  description?: string
  totalDays: number
  totalDistanceKm: number
  estimatedCost: EstimatedCost
  weatherAlert?: string
  itinerary: DayItinerary[]
  fuelStops: FuelStop[]
  createdAt: string
  startPoint?: string
  endPoint?: string
}

export interface RouteTemplate {
  id: number
  name: string
  region: string
  totalDays: number
  totalDistance?: number
  bestSeason?: string
  difficulty: number
  usageCount: number
}

export interface ApiResponse<T = any> {
  code: number
  message?: string
  data: T
}

export interface PageData<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface GeoData {
  lng: number
  lat: number
  address?: string
}

export interface RecordPayload {
  name: string
  type: string
  dayNumber: number
  note: string
  lng: number | null
  lat: number | null
  photoUrl: string | null
}

export interface RecordEntry {
  name: string
  type: string
  dayNumber: number
  note: string
  photo: string | null
}

export interface FuelData {
  fuelLeft: number
  fuelMax: number
  fuelPercent: number
  toEndKm: number
  stopsNeeded: number
}
