import { LanguageRounded, WindowRounded } from '@mui/icons-material'
import { Button, Stack } from '@mui/material'

export default function OAuthLoginButtons({ googleUrl, microsoftUrl }: { googleUrl?: string; microsoftUrl?: string }) {
  if (!googleUrl && !microsoftUrl) return null

  return (
    <Stack spacing={1.25}>
      {googleUrl && (
        <Button
          component="a"
          href={googleUrl}
          variant="outlined"
          size="large"
          startIcon={<LanguageRounded />}
          fullWidth
        >
          Entrar com Google
        </Button>
      )}
      {microsoftUrl && (
        <Button
          component="a"
          href={microsoftUrl}
          variant="outlined"
          size="large"
          startIcon={<WindowRounded />}
          fullWidth
        >
          Entrar com Microsoft
        </Button>
      )}
    </Stack>
  )
}
