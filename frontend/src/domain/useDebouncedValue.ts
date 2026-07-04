import { useEffect, useState } from 'react'

/** Genérico e reusável — não sabe nada sobre precificação/formulários. */
export function useDebouncedValue<T>(valor: T, atrasoMs: number): T {
  const [valorComAtraso, setValorComAtraso] = useState(valor)

  useEffect(() => {
    const timeoutId = setTimeout(() => setValorComAtraso(valor), atrasoMs)
    return () => clearTimeout(timeoutId)
  }, [valor, atrasoMs])

  return valorComAtraso
}
