import { act, renderHook } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import { SIDEBAR_COLLAPSED_KEY, useSidebarCollapsed } from '@/app/useSidebarCollapsed'

describe('useSidebarCollapsed', () => {
  afterEach(() => {
    localStorage.clear()
  })

  it('inicia expandido e persiste o estado recolhido', () => {
    const { result, unmount } = renderHook(() => useSidebarCollapsed())
    expect(result.current.collapsed).toBe(false)

    act(() => {
      result.current.toggleCollapsed()
    })
    expect(result.current.collapsed).toBe(true)
    expect(localStorage.getItem(SIDEBAR_COLLAPSED_KEY)).toBe('true')

    unmount()
    const again = renderHook(() => useSidebarCollapsed())
    expect(again.result.current.collapsed).toBe(true)
  })
})
