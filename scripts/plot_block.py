import math
import matplotlib.cm as cm
import matplotlib.pyplot as plt
import numpy as np
import os
import pylab as pl
import scipy as sp
import scipy.stats as stats
import sys

from array import *


KEYBOARD_TYPES_OUTPUT = ["on", "off", "wordwise", "arbitrary"]
PRETTY_TYPES = ["Screen on", "Screen off", "Wordwise", "User-controlled"]
COLORS = ["#1f77b4", "#2ca02c", "#d62728", "#ff7f0e"]

BLOCK_IDS = ["1", "2", "3", "4"]
BLOCK_NAMES = ["Block 1", "Block 2", "Block 3", "Block 4"]


def mean_confidence_interval(data, confidence=0.95):
    n = len(data)
    m, se = np.mean(data), stats.sem(data)
    h = se * stats.t._ppf((1 + confidence) / 2., n - 1)
    return h


def plot_bar_err(data, title):
    plt.close("all")
    fig, ax = plt.subplots(figsize=(10,5))
    means = []
    errs = []
    for i in xrange(0, len(KEYBOARD_TYPES_OUTPUT)):
        means.append([])
        errs.append([])
        for j in xrange(0, len(BLOCK_IDS)):
            means[i].append(np.mean(data[i][j]))
            errs[i].append(mean_confidence_interval(data[i][j]))

    N = len(BLOCK_NAMES)
    ind = np.arange(N)
    e3 = plt.errorbar(ind, means[2], yerr=errs[2], fmt='^-', color=COLORS[2],
                      label=PRETTY_TYPES[2], capsize=10, capthick=2,
                      elinewidth=2, mec=COLORS[2], lw=2, ms=10, zorder=3)
    e4 = plt.errorbar(ind, means[3], yerr=errs[3], fmt='v-', color=COLORS[3],
                      label=PRETTY_TYPES[3], capsize=10, capthick=2,
                      elinewidth=2, mec=COLORS[3], lw=2, ms=10, zorder=3)
    e1 = plt.errorbar(ind, means[0], yerr=errs[0], fmt='o-', color=COLORS[0],
                      label=PRETTY_TYPES[0], capsize=10, capthick=2,
                      elinewidth=2, mec=COLORS[0], lw=2, ms=10, zorder=4)
    e2 = plt.errorbar(ind, means[1], yerr=errs[1], fmt='s-', color=COLORS[1],
                      label=PRETTY_TYPES[1], capsize=10, capthick=2,
                      elinewidth=2, mec=COLORS[1], lw=2, ms=10, zorder=5)

    if title == "wpm":
        ax.set_ylabel("WPM", fontsize=14)
        # ax.set_yticks([0, 5, 10, 15, 20, 25])
    elif title == "wer":
        ax.set_ylabel("%", fontsize=14)
#        ax.set_yticks([0, 5, 10, 15])

    ax.set_xlim(-0.5, N - 0.5)
    ax.set_xticks(ind)
    ax.set_xticklabels(BLOCK_NAMES)
    plt.tick_params(axis='both', which='major', labelsize=14)
    plt.tick_params(axis='both', which='minor', labelsize=10)
    ax.yaxis.grid(True, which='major', color='#bdbdbd', lw=1, linestyle='-', zorder=0)
    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    ax.spines['bottom'].set_visible(True)
    ax.spines['bottom'].set_zorder(10)
    ax.spines['left'].set_visible(False)
    for tic in ax.xaxis.get_major_ticks():
        tic.tick1On = tic.tick2On = False
    for tic in ax.yaxis.get_major_ticks():
        tic.tick1On = tic.tick2On = False

    if title == "wpm":
        legend = ax.legend((e1[0], e2[0], e3[0], e4[0]), PRETTY_TYPES, fontsize=14,
                            frameon=True, loc='lower right',
                            bbox_to_anchor=(1, 0), ncol=2, numpoints=1)
    else:
        legend = ax.legend((e1[0], e2[0], e3[0], e4[0]), PRETTY_TYPES, fontsize=14,
                            frameon=False, loc='upper right',
                            bbox_to_anchor=(1, 1.05), ncol=1, numpoints=1)

    plt.savefig("./results/user_block_mean_" + title + ".png", bbox_inches='tight')
    plt.savefig("./results/user_block_mean_" + title + ".pdf", format='pdf', bbox_inches='tight')


def main():
    wpm = []
    wer = []
    for i in xrange(0, len(KEYBOARD_TYPES_OUTPUT)):
        wpm.append([])
        wer.append([])
        for block in BLOCK_IDS:
            wpm[i].append([])
            wer[i].append([])

    input_f = open('results/anova_user_block_mean.txt', 'r')
    for line in input_f:
        if line.startswith("user") == True:
            continue
        s = line.strip().split('\t')
        keyboard = s[5].strip()
        keyboard_index = KEYBOARD_TYPES_OUTPUT.index(keyboard)
        block = BLOCK_IDS.index(s[1].strip())
        wpm[keyboard_index][block].append(float(s[2].strip()))
        wer[keyboard_index][block].append(float(s[3].strip()))

    input_f.close()

    plot_bar_err(wpm, "wpm")
    plot_bar_err(wer, "wer")


if __name__ == '__main__':
  main()
