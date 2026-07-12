import { useEffect, useMemo, useState } from 'react'
import { Alert, Box, Button, Card, CardContent, Checkbox, FormControl, InputLabel, MenuItem, Select, Stack, TextField, Typography } from '@mui/material'
import { ApiError } from './api'
import { frequenciaApi, type AdolescenteResumo, type Encontro, type SituacaoEncontro, type SituacaoFrequencia } from './frequenciaApi'

interface Props { discipuladoId:number; podeAdministrar?:boolean }
export default function FrequencyManagement({discipuladoId,podeAdministrar=false}:Props){
 const [encontros,setEncontros]=useState<Encontro[]>([]),[selecionado,setSelecionado]=useState<Encontro>(),[adolescentes,setAdolescentes]=useState<AdolescenteResumo[]>([])
 const [chamada,setChamada]=useState<Record<number,SituacaoFrequencia>>({}),[data,setData]=useState(new Date().toISOString().slice(0,10)),[situacao,setSituacao]=useState<SituacaoEncontro>('REALIZADO')
 const [visitantes,setVisitantes]=useState(0),[erro,setErro]=useState(''),[sucesso,setSucesso]=useState(''),[carregando,setCarregando]=useState(false)
 const editavel=useMemo(()=>selecionado&&selecionado.situacao==='REALIZADO'&&(podeAdministrar||Date.now()<=new Date(selecionado.criadoEm).getTime()+3*60*60*1000),[selecionado,podeAdministrar])
 async function carregar(){setErro('');try{setEncontros(await frequenciaApi.listarEncontros(discipuladoId));const p=await frequenciaApi.listarAdolescentes(discipuladoId);setAdolescentes(p.content)}catch(e){setErro(mensagem(e))}}
 useEffect(()=>{void carregar()},[discipuladoId])
 async function abrir(e:Encontro){setSelecionado(e);setErro('');try{const existentes=await frequenciaApi.listarChamada(e.id);const mapa:Record<number,SituacaoFrequencia>={};adolescentes.forEach(a=>mapa[a.id]='AUSENTE');existentes.forEach(f=>mapa[f.adolescenteId]=f.situacao);setChamada(mapa)}catch(x){setErro(mensagem(x))}}
 async function criar(){setCarregando(true);setErro('');try{const novo=await frequenciaApi.criarEncontro({discipuladoId,data,situacao});setSucesso('Encontro criado.');await carregar();await abrir(novo)}catch(e){setErro(mensagem(e))}finally{setCarregando(false)}}
 async function salvar(){if(!selecionado)return;setCarregando(true);setErro('');try{await frequenciaApi.salvarChamada(selecionado.id,adolescentes.map(a=>({adolescenteId:a.id,situacao:chamada[a.id]??'AUSENTE'})));await frequenciaApi.salvarVisitantes(selecionado.id,visitantes);setSucesso('Chamada salva.')}catch(e){setErro(mensagem(e))}finally{setCarregando(false)}}
 return <Stack spacing={2}>
  <Typography variant="h5">Encontros e frequência</Typography>{erro&&<Alert severity="error">{erro}</Alert>}{sucesso&&<Alert severity="success">{sucesso}</Alert>}
  <Card><CardContent><Stack direction={{xs:'column',sm:'row'}} spacing={2}><TextField label="Data" type="date" value={data} onChange={e=>setData(e.target.value)} slotProps={{inputLabel:{shrink:true}}}/><FormControl sx={{minWidth:180}}><InputLabel>Situação</InputLabel><Select label="Situação" value={situacao} onChange={e=>setSituacao(e.target.value as SituacaoEncontro)}><MenuItem value="REALIZADO">Realizado</MenuItem><MenuItem value="CANCELADO">Cancelado</MenuItem></Select></FormControl><Button variant="contained" onClick={criar} disabled={carregando}>Criar encontro</Button></Stack></CardContent></Card>
  <FormControl fullWidth><InputLabel>Encontro</InputLabel><Select label="Encontro" value={selecionado?.id??''} onChange={e=>{const item=encontros.find(x=>x.id===Number(e.target.value));if(item)void abrir(item)}}>{encontros.map(e=><MenuItem key={e.id} value={e.id}>{e.data} — {e.situacao}</MenuItem>)}</Select></FormControl>
  {selecionado&&<Card><CardContent><Stack spacing={1}>{!editavel&&<Alert severity="info">Chamada em modo somente leitura: encontro cancelado ou janela de três horas encerrada.</Alert>}{adolescentes.map(a=><Box key={a.id} display="flex" alignItems="center"><Checkbox checked={chamada[a.id]==='PRESENTE'} disabled={!editavel} onChange={e=>setChamada(c=>({...c,[a.id]:e.target.checked?'PRESENTE':'AUSENTE'}))}/><Typography>{a.nome}</Typography></Box>)}<TextField label="Visitantes" type="number" value={visitantes} disabled={!editavel} onChange={e=>setVisitantes(Math.max(0,Number(e.target.value)))} inputProps={{min:0}}/><Button variant="contained" disabled={!editavel||carregando} onClick={salvar}>Salvar chamada</Button></Stack></CardContent></Card>}
 </Stack>
}
function mensagem(e:unknown){return e instanceof ApiError?e.message:'Não foi possível concluir a operação.'}
