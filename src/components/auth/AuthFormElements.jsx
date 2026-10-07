import React, { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

export function AuthField({ label, ...inputProps }) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-sm font-medium text-foreground" htmlFor={inputProps.id}>
        {label}
      </label>
      <input
        {...inputProps}
        className="h-9 rounded-lg border border-border bg-white px-3 text-sm outline-none focus:border-primary focus:ring-2 focus:ring-primary/30"
      />
    </div>
  );
}

export function AuthPasswordField({ label, ...inputProps }) {
  const [visible, setVisible] = useState(false);
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-sm font-medium text-foreground" htmlFor={inputProps.id}>
        {label}
      </label>
      <div className="relative">
        <input
          {...inputProps}
          type={visible ? 'text' : 'password'}
          className="h-9 w-full rounded-lg border border-border bg-white pl-3 pr-10 text-sm outline-none focus:border-primary focus:ring-2 focus:ring-primary/30"
        />
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? 'Hide password' : 'Show password'}
          className="absolute inset-y-0 right-0 flex w-9 items-center justify-center bg-transparent p-0 text-black shadow-none hover:translate-y-0 hover:text-black/70 hover:shadow-none"
        >
          {visible ? <EyeOff size={16} className="shrink-0" /> : <Eye size={16} className="shrink-0" />}
        </button>
      </div>
    </div>
  );
}

export function AuthError({ children }) {
  if (!children) return null;
  return (
    <div className="rounded-lg border border-[#fecaca] bg-[#fef2f2] px-3 py-2 text-sm text-[#b91c1c]">{children}</div>
  );
}

export function AuthSuccess({ children }) {
  if (!children) return null;
  return (
    <div className="rounded-lg border border-[#bbf7d0] bg-[#f0fdf4] px-3 py-2 text-sm text-[#15803d]">{children}</div>
  );
}

export function AuthSubmitButton({ children, ...buttonProps }) {
  return (
    <button
      {...buttonProps}
      type="submit"
      className="mt-2 h-9 rounded-lg bg-primary text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-60"
    >
      {children}
    </button>
  );
}

export function AuthLinkRow({ children }) {
  return <p className="mt-4 text-center text-sm text-muted-foreground">{children}</p>;
}
