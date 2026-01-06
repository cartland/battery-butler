Create a "Edit Device Type" screen that shows the name, battery type, and battery quantity as fields to edit.

<!DOCTYPE html>

<html class="dark" lang="en"><head>
<meta charset="utf-8"/>
<meta content="width=device-width, initial-scale=1.0" name="viewport"/>
<title>Edit Device Type</title>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&amp;display=swap" rel="stylesheet"/>
<link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&amp;display=swap" rel="stylesheet"/>
<link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&amp;display=swap" rel="stylesheet"/>
<script src="https://cdn.tailwindcss.com?plugins=forms,container-queries"></script>
<script id="tailwind-config">
        tailwind.config = {
            darkMode: "class",
            theme: {
                extend: {
                    colors: {
                        "primary": "#137fec",
                        "background-light": "#f6f7f8",
                        "background-dark": "#101922",
                    },
                    fontFamily: {
                        "display": ["Inter", "sans-serif"]
                    },
                    borderRadius: { "DEFAULT": "0.25rem", "lg": "0.5rem", "xl": "0.75rem", "full": "9999px" },
                },
            },
        }
    </script>
<style>
    body {
      min-height: max(884px, 100dvh);
    }
  </style>
  </head>
<body class="bg-background-light dark:bg-background-dark font-display min-h-screen flex flex-col text-slate-900 dark:text-white overflow-x-hidden selection:bg-primary/30">
<!-- Top Navigation Bar -->
<!-- Based on TopAppBar component, modified for iOS modality -->
<header class="sticky top-0 z-50 w-full bg-background-light/80 dark:bg-background-dark/80 backdrop-blur-md border-b border-slate-200 dark:border-slate-800 transition-colors duration-300">
<div class="flex items-center justify-between px-4 h-14 max-w-2xl mx-auto">
<button class="text-primary text-[17px] font-normal hover:opacity-70 transition-opacity flex items-center min-w-[60px]">
                Cancel
            </button>
<h1 class="text-[17px] font-semibold text-center flex-1 truncate px-2">
                Edit Device Type
            </h1>
<button class="text-primary text-[17px] font-bold hover:opacity-70 transition-opacity flex justify-end min-w-[60px]">
                Save
            </button>
</div>
</header>
<!-- Main Content -->
<main class="flex-1 w-full max-w-2xl mx-auto p-4 md:p-6 flex flex-col gap-6">
<!-- Form Container (Inset Grouped Style) -->
<div class="bg-white dark:bg-[#1C252E] rounded-xl overflow-hidden shadow-sm border border-slate-200 dark:border-slate-800/60">
<!-- Device Name Input -->
<!-- Based on TextField component, modified for list layout -->
<div class="group flex flex-col sm:flex-row sm:items-center justify-between p-4 border-b border-slate-100 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-[#232d38] transition-colors relative">
<label class="text-base font-medium text-slate-900 dark:text-white mb-1 sm:mb-0 sm:min-w-[120px]" for="device-name">Name</label>
<input class="form-input flex-1 bg-transparent border-none p-0 text-left sm:text-right text-base text-slate-600 dark:text-slate-400 focus:text-primary focus:ring-0 placeholder:text-slate-400 font-normal" id="device-name" placeholder="Enter device name" type="text" value="Living Room Remote"/>
</div>
<!-- Battery Type Selector -->
<!-- Based on TextField (Select) component -->
<div class="group flex flex-col sm:flex-row sm:items-center justify-between p-4 border-b border-slate-100 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-[#232d38] transition-colors relative">
<label class="text-base font-medium text-slate-900 dark:text-white mb-1 sm:mb-0 sm:min-w-[120px]" for="battery-type">Battery Type</label>
<div class="flex items-center sm:justify-end flex-1 relative">
<select class="form-select w-full sm:w-auto appearance-none bg-transparent border-none p-0 pr-8 text-left sm:text-right text-base text-slate-600 dark:text-slate-400 focus:text-primary focus:ring-0 cursor-pointer z-10" id="battery-type">
<option class="bg-white dark:bg-[#1C252E]" value="AA">AA</option>
<option class="bg-white dark:bg-[#1C252E]" value="AAA">AAA</option>
<option class="bg-white dark:bg-[#1C252E]" value="CR2032">CR2032</option>
<option class="bg-white dark:bg-[#1C252E]" value="9V">9V</option>
</select>
<span class="material-symbols-outlined absolute right-0 text-slate-400 pointer-events-none text-xl">chevron_right</span>
</div>
</div>
<!-- Quantity Stepper -->
<!-- Based on ListItem component -->
<div class="flex flex-row items-center justify-between p-4 hover:bg-slate-50 dark:hover:bg-[#232d38] transition-colors">
<label class="text-base font-medium text-slate-900 dark:text-white">Quantity</label>
<div class="flex items-center gap-4">
<div class="flex items-center gap-1 bg-slate-100 dark:bg-[#111920] rounded-lg p-1">
<button class="w-8 h-8 flex items-center justify-center rounded bg-white dark:bg-[#283039] text-slate-900 dark:text-white shadow-sm hover:bg-slate-50 dark:hover:bg-[#323b46] active:scale-95 transition-all" type="button">
<span class="material-symbols-outlined text-lg">remove</span>
</button>
<input class="w-10 bg-transparent border-none text-center text-base font-semibold text-slate-900 dark:text-white p-0 focus:ring-0 [appearance:textfield] [&amp;::-webkit-outer-spin-button]:appearance-none [&amp;::-webkit-inner-spin-button]:appearance-none" type="number" value="2"/>
<button class="w-8 h-8 flex items-center justify-center rounded bg-white dark:bg-[#283039] text-slate-900 dark:text-white shadow-sm hover:bg-slate-50 dark:hover:bg-[#323b46] active:scale-95 transition-all" type="button">
<span class="material-symbols-outlined text-lg">add</span>
</button>
</div>
</div>
</div>
</div>
<p class="px-2 text-xs text-slate-500 dark:text-slate-400 font-medium uppercase tracking-wider">
            Configuration
        </p>
<!-- Delete Action -->
<!-- Based on SingleButton component -->
<button class="group w-full bg-white dark:bg-[#1C252E] flex items-center justify-center gap-2.5 p-4 rounded-xl shadow-sm border border-slate-200 dark:border-slate-800/60 active:scale-[0.99] transition-all hover:bg-red-50 dark:hover:bg-red-900/10">
<span class="material-symbols-outlined text-red-500 text-2xl group-hover:scale-110 transition-transform">delete</span>
<span class="text-red-500 text-base font-semibold tracking-wide">Delete Device Type</span>
</button>
</main>
<!-- Bottom Spacing for safe area on mobile -->
<div class="h-6 w-full"></div>
</body></html>