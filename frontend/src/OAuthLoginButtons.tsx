import { Button, Stack } from '@mui/material'

export default function OAuthLoginButtons({ googleUrl, microsoftUrl }: { googleUrl?: string; microsoftUrl?: string }) {
  if (!googleUrl && !microsoftUrl) return null
  return <Stack spacing={1}>{googleUrl && <Button component="a" href={googleUrl} variant="outlined">Entrar com Google</Button>}{microsoftUrl && <Button component="a" href={microsoftUrl} variant="outlined">Entrar com Microsoft</Button>}</Stack>
}
