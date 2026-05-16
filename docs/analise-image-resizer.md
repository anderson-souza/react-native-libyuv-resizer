# Análise: bamlab/react-native-image-resizer vs rn-libyuv-resizer

> Referência: https://github.com/bamlab/react-native-image-resizer
> Data: 2026-05-16

## O que bamlab tem — ausente neste projeto

| #   | Funcionalidade                        | Detalhe                                                                                                        |
| --- | ------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| 1   | **Retorno rico** ✅ implementado      | bamlab retorna `{ path, uri, size, name, width, height }`. Implementado em `feat/rich-return-type`.            |
| 2   | **`onlyScaleDown`**                   | Impede upscale — imagem menor que target não é ampliada.                                                       |
| 3   | **`keepMeta` / cópia de EXIF**        | Copia ~80 tags EXIF (GPS, câmera, data, etc.) do original para o output. Crítico para apps de foto.            |
| 4   | **Formato PNG de saída**              | bamlab suporta `JPEG \| PNG \| WEBP`. Este projeto é JPEG-only (exceto quality=100 → PNG interno).             |
| 5   | **Formato WEBP de saída**             | Idem acima.                                                                                                    |
| 6   | **Input por URI remoto (http/https)** | bamlab baixa a imagem via HTTP antes de redimensionar.                                                         |
| 7   | **Input por `content://`**            | bamlab lê via `ContentResolver` (galeria, MediaStore). Este projeto exige path absoluto.                       |
| 8   | **Input por base64 data URI**         | `data:image/jpeg;base64,...` como source.                                                                      |
| 9   | **Auto-orientação EXIF**              | bamlab lê orientação EXIF e corrige automaticamente antes de redimensionar.                                    |
| 10  | **iOS nativo**                        | bamlab tem impl completa em ObjC++ com `RCTImageLoader`/`CoreGraphics`. iOS aqui é stub (`E_NOT_IMPLEMENTED`). |
| 11  | **`base64` no retorno**               | Campo opcional com conteúdo base64 do arquivo gerado. Útil para upload direto.                                 |
| 12  | **Suporte legacy bridge**             | bamlab tem `oldarch/` + `newarch/`. Este projeto é New Arch only (intencional).                                |

## O que este projeto tem — bamlab não tem

| #   | Funcionalidade         | Detalhe                                                                            |
| --- | ---------------------- | ---------------------------------------------------------------------------------- |
| 1   | **libyuv backend**     | Backend nativo de alto desempenho no Android via C++/JNI em vez de `Bitmap` Java.  |
| 2   | **`filterMode`**       | `none \| linear \| bilinear \| box` — controle de qualidade/velocidade do scaling. |
| 3   | **Web fallback**       | `resizer.tsx` rejeita graciosamente em plataformas não-nativas.                    |
| 4   | **TypeScript estrito** | `RotationAngle`, `ResizeMode`, `FilterMode` como unions literais; sem `any`.       |
| 5   | **New Arch only**      | Turbo Module puro — sem overhead de bridge legacy.                                 |

## Backlog priorizado

### Alta — afeta casos de uso comuns

- [ ] `onlyScaleDown` — evitar upscale acidental
- [ ] Formato PNG/WEBP de saída explícito no parâmetro
- [ ] Input `content://` (galeria Android)

### Média — nichos importantes

- [x] Auto-orientação EXIF (ler e corrigir antes de redimensionar)
- [ ] `keepMeta` — cópia de tags EXIF para o output
- [ ] Input por URL remoto (http/https)
- [ ] iOS nativo com `vImage` (Accelerate framework)

### Baixa — raramente necessário

- [ ] `base64` no retorno
- [ ] Input por base64 data URI
