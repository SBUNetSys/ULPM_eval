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
COLORS = ["#1f77b4", "#1f77b4", "#1f77b4", "#1f77b4"]


def mean_confidence_interval(data, confidence=0.95):
    n = len(data)
    m, se = np.mean(data), stats.sem(data)
    h = se * stats.t._ppf((1 + confidence) / 2., n - 1)
    return h


def plot_bar_err(data, title):
    plt.close("all")
    fig, ax = plt.subplots(figsize=(10,4))
    means = []
    errs = []
    for i in xrange(0, len(PRETTY_TYPES)):
        means.append(np.mean(data[i]))
        errs.append(mean_confidence_interval(data[i]))

    N = len(PRETTY_TYPES)
    ind = np.arange(N)
    width = 0.46
    rects0 = ax.bar(ind[0], means[0], width, color=COLORS[0], alpha=1, ec='white',
                    yerr=errs[0], error_kw={'ecolor':'black', 'linewidth':2, 'zorder':4}, capsize=10, zorder=3)
    rects1 = ax.bar(ind[1], means[1], width, color=COLORS[1], alpha=1, ec='white',
                    yerr=errs[1], error_kw={'ecolor':'black', 'linewidth':2, 'zorder':4}, capsize=10, zorder=3)
    rects2 = ax.bar(ind[2], means[2], width, color=COLORS[2], alpha=1, ec='white',
                    yerr=errs[2], error_kw={'ecolor':'black', 'linewidth':2, 'zorder':4}, capsize=10, zorder=3)
    rects3 = ax.bar(ind[3], means[3], width, color=COLORS[3], alpha=1, ec='white',
                    yerr=errs[3], error_kw={'ecolor':'black', 'linewidth':2, 'zorder':4}, capsize=10, zorder=3)

    if title == "wpm":
        ax.set_ylabel("WPM", fontsize=14)
    elif title == "wer":
        ax.set_ylabel("%", fontsize=14)

    ax.set_xlim(-0.27, N - 0.27)
    ax.set_xticks(ind + 0.23)
    ax.set_xticklabels(PRETTY_TYPES)
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

    def autolabel(rects):
        for rect in rects:
            height = rect.get_height()
            ax.text(rect.get_x() + rect.get_width()/1.1, height * 1.03,
                    '%.2f' % height, ha='center', va='bottom', fontsize=14, zorder=4)

    autolabel(rects0)
    autolabel(rects1)
    autolabel(rects2)
    autolabel(rects3)
    plt.savefig("./results/user_mean_" + title + ".png", bbox_inches='tight')
    plt.savefig("./results/user_mean_" + title + ".pdf", format='pdf', bbox_inches='tight')


def main():
    wpm = []
    wer = []
    for i in xrange(0, len(PRETTY_TYPES)):
        wpm.append([])
        wer.append([])

    input_f = open('results/anova_user_mean.txt', 'r')
    for line in input_f:
        if line.startswith("user") == True:
            continue
        s = line.strip().split('\t')
        keyboard_version = s[4].strip()
        keyboard_index = KEYBOARD_TYPES_OUTPUT.index(keyboard_version)
        wpm[keyboard_index].append(float(s[1].strip()))
        wer[keyboard_index].append(float(s[2].strip()))
    input_f.close()

    plot_bar_err(wpm, "wpm")
    plot_bar_err(wer, "wer")


if __name__ == '__main__':
  main()
