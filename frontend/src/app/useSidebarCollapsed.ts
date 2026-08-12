import { useCallback, useEffect, useState } from 'react'

export const SIDEBAR_COLLAPSED_KEY = 'sgd:sidebar-collapsed'

export function readSidebarCollapsed(): boolean {
  if (typeof window === 'undefined') return false
  return window.localStorage.getItem(SIDEBAR_COLLAPSED_KEY) === 'true'
}

export function useSidebarCollapsed() {
  const [collapsed, setCollapsed] = useState(readSidebarCollapsed)

  useEffect(() => {
    window.localStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(collapsed))
  }, [collapsed])

  const toggleCollapsed = useCallback(() => {
    setCollapsed((current) => !current)
  }, [])

  return { collapsed, toggleCollapsed }
}
