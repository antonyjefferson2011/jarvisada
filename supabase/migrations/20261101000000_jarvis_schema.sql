create extension if not exists pgcrypto;

create table if not exists public.jarvis_messages (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    conversation_id uuid not null default '00000000-0000-0000-0000-000000000001',
    role text not null check (role in ('user', 'assistant')),
    content text not null check (length(content) between 1 and 6000),
    created_at timestamptz not null default now()
);

create index if not exists jarvis_messages_user_created_idx
    on public.jarvis_messages (user_id, created_at);

alter table public.jarvis_messages enable row level security;

drop policy if exists "Users manage their own Jarvis messages"
    on public.jarvis_messages;
create policy "Users manage their own Jarvis messages"
    on public.jarvis_messages
    for all
    to authenticated
    using ((select auth.uid()) = user_id)
    with check ((select auth.uid()) = user_id);

grant select, insert, update, delete on public.jarvis_messages to authenticated;

create table if not exists public.jarvis_memories (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    content text not null check (length(content) between 1 and 400),
    created_at timestamptz not null default now()
);

create index if not exists jarvis_memories_user_created_idx
    on public.jarvis_memories (user_id, created_at desc);

alter table public.jarvis_memories enable row level security;

drop policy if exists "Users manage their own Jarvis memories"
    on public.jarvis_memories;
create policy "Users manage their own Jarvis memories"
    on public.jarvis_memories
    for all
    to authenticated
    using ((select auth.uid()) = user_id)
    with check ((select auth.uid()) = user_id);

grant select, insert, update, delete on public.jarvis_memories to authenticated;
