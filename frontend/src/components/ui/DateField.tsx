import { forwardRef, type InputHTMLAttributes } from 'react'
import { Input } from './Input'

type DateFieldProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> & {
  label: string
  error?: string
}

export const DateField = forwardRef<HTMLInputElement, DateFieldProps>(function DateField(props, ref) {
  return <Input ref={ref} type="date" {...props} />
})
