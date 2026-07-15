import { AddRounded, EventRounded, FactCheckRounded, SaveRounded } from '@mui/icons-material'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Alert, Box, Button, Checkbox, Chip, FormControl, InputLabel, MenuItem, Select, Stack, TextField, Typography } from '@mui/material'
import { ApiError } from './api'
import { frequenciaApi, type AdolescenteResumo, type Encontro, type SituacaoEncontro, type SituacaoFrequencia } from './frequenciaApi'
import { EmptyState, SectionCard } from './ui'

interface Props { discipuladoId:number; podeAdministrar?:boolean }
interface ParticipanteChamada extends AdolescenteResumo { registroAnterior:boolean }

export default function FrequencyManagement({discipuladoId,podeAdministrar=false}:Props){
 const [encontros,setEncontros]=useState<Encontro[]>([]),[selecionado,setSelecionado]=useState<Encontro>(),[adolescentesAtuais,setAdolescentesAtuais]=useState<AdolescenteResumo[]>([]),[participantes,setParticipantes]=useState<ParticipanteChamada[]>([])
 const [chamada,setChamada]=useState<Record<number,SituacaoFrequencia>>({}),[data,setData]=useState(new Date().toISOString().slice(0,10)),[situacao,setSituacao]=useState<SituacaoEncontro>('REALIZADO')
 const [visitantes,setVisitantes]=useState(0),[erro,setErro]=useState(''),[sucesso,setSucesso]=useState(''),[carregando,setCarregando]=useState(false)
 const editavel=useMemo(()=>Boolean(selecionado&&selecionado.situacao==='REALIZADO'&&(podeAdministrar||Date.now()<=new Date(selecionado.criadoEm).getTime()+3*60*60*1000)),[selecionado,podeAdministrar])
 const carregar=useCallback(async()=>{const [novosEncontros,pagina]=await Promise.all([frequenciaApi.listarEncontros(discipuladoId),frequenciaApi.listarAdolescentes(discipuladoId)]);setEncontros(novosEncontros);setAdolescentesAtuais(pagina.content);return pagina.content},[discipuladoId])
 useEffect(()=>{setSelecionado(undefined);setParticipantes([]);setChamada({});setErro('');setSucesso('');void carregar().catch(e=>setErro(mensagem(e)))},[carregar])
 async function abrir(e:Encontro,atuais=adolescentesAtuais){setSelecionado(e);setVisitantes(0);setErro('');try{const existentes=await frequenciaApi.listarChamada(e.id);const idsAtuais=new Set(atuais.map(a=>a.id));const lista:ParticipanteChamada[]=[...atuais.map(a=>({...a,registroAnterior:false})),...existentes.filter(f=>!idsAtuais.has(f.adolescenteId)).map(f=>({id:f.adolescenteId,nome:f.adolescenteNome,registroAnterior:true}))];const mapa:Record<number,SituacaoFrequencia>={};lista.forEach(a=>mapa[a.id]='AUSENTE');existentes.forEach(f=>mapa[f.adolescenteId]=f.situacao);setParticipantes(lista);setChamada(mapa)}catch(x){setErro(mensagem(x));throw x}}
 async function criar(){setCarregando(true);setErro('');setSucesso('');try{const novo=await frequenciaApi.criarEncontro({discipuladoId,data,situacao});setSucesso('Encontro criado.');try{const atuais=await carregar();await abrir(novo,atuais)}catch{setSelecionado(novo);setErro('O encontro foi criado, mas não foi possível carregar a chamada. Selecione-o novamente para tentar de novo.')}}catch(e){setErro(mensagem(e))}finally{setCarregando(false)}}
 async function salvar(){if(!selecionado)return;setCarregando(true);setErro('');setSucesso('');try{await frequenciaApi.salvarChamada(selecionado.id,participantes.map(a=>({adolescenteId:a.id,situacao:chamada[a.id]??'AUSENTE'})));await frequenciaApi.salvarVisitantes(selecionado.id,visitantes);setSucesso('Chamada salva.')}catch(e){setErro(mensagem(e))}finally{setCarregando(false)}}
 return <Stack spacing={3}>
  {erro&&<Alert severity="error" onClose={()=>setErro('')}>{erro}</Alert>}{sucesso&&<Alert severity="success" onClose={()=>setSucesso('')}>{sucesso}</Alert>}
  <Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr',lg:'minmax(360px,.7fr) minmax(420px,1.3fr)'},gap:3}}>
   <SectionCard title="Novo encontro" description="Informe a data e a situação para iniciar uma chamada." icon={<EventRounded />}>
    <Stack spacing={2}><TextField fullWidth label="Data" type="date" value={data} onChange={e=>setData(e.target.value)} slotProps={{inputLabel:{shrink:true}}}/><FormControl fullWidth><InputLabel>Situação</InputLabel><Select label="Situação" value={situacao} onChange={e=>setSituacao(e.target.value as SituacaoEncontro)}><MenuItem value="REALIZADO">Realizado</MenuItem><MenuItem value="CANCELADO">Cancelado</MenuItem></Select></FormControl><Button variant="contained" startIcon={<AddRounded />} onClick={criar} disabled={carregando}>Criar encontro</Button></Stack>
   </SectionCard>
   <SectionCard title="Encontros registrados" description="Selecione um encontro para consultar ou editar a chamada." icon={<FactCheckRounded />}>
    {encontros.length?<FormControl fullWidth><InputLabel>Encontro</InputLabel><Select label="Encontro" value={selecionado?.id??''} onChange={e=>{const item=encontros.find(x=>x.id===Number(e.target.value));if(item)void abrir(item).catch(()=>undefined)}}>{encontros.map(e=><MenuItem key={e.id} value={e.id}>{formatarData(e.data)} — {e.situacao==='REALIZADO'?'Realizado':'Cancelado'}</MenuItem>)}</Select></FormControl>:<EmptyState title="Nenhum encontro registrado" description="Crie o primeiro encontro para iniciar a chamada." />}
   </SectionCard>
  </Box>
  {selecionado&&<SectionCard title={`Chamada de ${formatarData(selecionado.data)}`} description={editavel?'Marque os participantes presentes e informe o número de visitantes.':'Consulta em modo somente leitura.'} icon={<FactCheckRounded />} action={<Chip size="small" color={selecionado.situacao==='REALIZADO'?'success':'default'} label={selecionado.situacao==='REALIZADO'?'Realizado':'Cancelado'} />}>
   <Stack spacing={2.5}>
    {!editavel&&<Alert severity="info">Chamada em modo somente leitura: encontro cancelado ou janela de três horas encerrada.</Alert>}
    {participantes.length?<Box sx={{display:'grid',gridTemplateColumns:{xs:'1fr',md:'repeat(2,minmax(0,1fr))'},gap:1}}>{participantes.map(a=><Box key={a.id} sx={{display:'flex',alignItems:'center',gap:1,p:1.25,border:'1px solid',borderColor:chamada[a.id]==='PRESENTE'?'success.light':'divider',borderRadius:2,bgcolor:chamada[a.id]==='PRESENTE'?'#F1F8F2':'background.paper'}}><Checkbox checked={chamada[a.id]==='PRESENTE'} disabled={!editavel} onChange={e=>setChamada(c=>({...c,[a.id]:e.target.checked?'PRESENTE':'AUSENTE'}))} inputProps={{'aria-label':`Marcar presença de ${a.nome}`}}/><Box minWidth={0} flexGrow={1}><Typography variant="body2" fontWeight={600}>{a.nome}</Typography><Typography variant="caption" color="text.secondary">{chamada[a.id]==='PRESENTE'?'Presente':'Ausente'}</Typography></Box>{a.registroAnterior&&<Chip size="small" variant="outlined" label="Registro anterior" />}</Box>)}</Box>:<EmptyState title="Nenhum participante disponível" description="Não há adolescentes vinculados a esta chamada." />}
    <Stack direction={{xs:'column',sm:'row'}} alignItems={{sm:'center'}} justifyContent="space-between" gap={2} sx={{pt:2,borderTop:'1px solid',borderColor:'divider'}}><TextField label="Visitantes" type="number" value={visitantes} disabled={!editavel} onChange={e=>setVisitantes(Math.max(0,Number(e.target.value)))} inputProps={{min:0}} sx={{maxWidth:{sm:220}}}/><Button variant="contained" startIcon={<SaveRounded />} disabled={!editavel||carregando} onClick={salvar}>Salvar chamada</Button></Stack>
   </Stack>
  </SectionCard>}
 </Stack>
}

function mensagem(e:unknown){return e instanceof ApiError?e.message:'Não foi possível concluir a operação.'}
function formatarData(data:string){return new Intl.DateTimeFormat('pt-BR').format(new Date(`${data}T12:00:00`))}
