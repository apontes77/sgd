import { Skeleton, Stack, Typography } from '@mui/material'

export function LoadingState({ label = 'Carregando...' }: { label?: string }) {
  return (
    <Stack role="status" aria-label={label} spacing={1.25} sx={{ py: 3 }}>
      <Skeleton variant="rounded" height={72} sx={{ borderRadius: '12px' }} />
      <Skeleton variant="rounded" height={72} sx={{ borderRadius: '12px' }} />
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
    </Stack>
  )
}
