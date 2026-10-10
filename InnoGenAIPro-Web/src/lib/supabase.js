import { createClient } from '@supabase/supabase-js';

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || '';
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY || 'sb_publishable__Mm3P8lwzjyHiD1DIloP_w_zrn8L7FQ';

export const isSupabaseConfigured = () => {
  return Boolean(supabaseUrl && supabaseUrl.startsWith('https://') && supabaseAnonKey);
};

export const supabase = isSupabaseConfigured()
  ? createClient(supabaseUrl, supabaseAnonKey, {
      realtime: {
        params: {
          eventsPerSecond: 10
        }
      }
    })
  : null;
