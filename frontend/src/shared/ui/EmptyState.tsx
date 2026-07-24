import { Stack, Typography } from '@mui/material'

export function EmptyState({
  title,
  description = 'Não há dados para exibir.',
}: {
  title: string
  description?: string
}) {
  return (
    <Stack alignItems="center" textAlign="center" spacing={0.75} sx={{ py: 5, px: 2 }}>
      <Typography variant="h6">{title}</Typography>
      <Typography variant="body2" color="text.secondary">
        {description}
      </Typography>
    </Stack>
  )
}
