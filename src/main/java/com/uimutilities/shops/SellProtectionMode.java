package com.uimutilities.shops;

/**
 * Which items a shop is allowed to buy. A block list only protects what someone thought to list; an
 * allow list protects everything else by default, at the cost of approving each routine sale.
 */
public enum SellProtectionMode
{
	OFF("Off"),
	BLOCK_LISTED("Block listed items"),
	ALLOW_LISTED_ONLY("Only allow listed items");

	private final String label;

	SellProtectionMode(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
